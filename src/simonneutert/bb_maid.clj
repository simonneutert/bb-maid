#!/usr/bin/env bb

(ns simonneutert.bb-maid
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [bling.core :refer [bling callout]]))

(defn extract-date [filename]
  (when-let [cleanup-file (re-find #"cleanup-maid-\d{4}-\d{2}-\d{2}" filename)]
    (when-let [date-str (re-find #"\d{4}-\d{2}-\d{2}" cleanup-file)]
      date-str)))

(defn past-date? [date-str]
  (let [file-date (java.time.LocalDate/parse date-str)
        current-date (java.time.LocalDate/now)]
    (.isBefore file-date current-date)))

(defn parse-duration
  "Parse duration string like '7d' and return number of days"
  [duration-str]
  (when-let [match (re-matches #"(\d+)d" duration-str)]
    (Integer/parseInt (second match))))

(defn calculate-future-date
  "Calculate a future date by adding days to today"
  [days]
  (let [current-date (java.time.LocalDate/now)]
    (.plusDays current-date days)))

(defn remove-existing-cleanup-files
  "Remove any existing cleanup-maid files in the specified directory"
  [dir]
  (let [existing-files (filter #(re-matches #"cleanup-maid-\d{4}-\d{2}-\d{2}" (fs/file-name %))
                               (fs/list-dir dir))]
    (doseq [file existing-files]
      (callout {:type :warning} (bling "Removing existing cleanup file: " [:bold (fs/file-name file)]))
      (fs/delete file))))

(defn create-cleanup-file
  "Create a cleanup file with a date based on the duration in the specified directory"
  [duration-str dir]
  (if-let [days (parse-duration duration-str)]
    (do
      (remove-existing-cleanup-files dir)
      (let [future-date (calculate-future-date days)
            date-str (.format future-date (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))
            filepath (str (fs/path dir (str "cleanup-maid-" date-str)))]
        (callout {:type :info} (bling "Creating cleanup file: " [:bold filepath] " (expires in " [:bold (str days)] " days)"))
        (spit filepath "")
        (callout {:type :success} (bling [:green "✓"] " File created successfully"))))
    (callout {:type :error} (bling [:red "Error:"] " Invalid duration format. Use format like '7d' for 7 days"))))

(defn add-to-gitignore
  "Add cleanup-maid-* pattern to .gitignore file in the specified directory"
  [dir]
  (let [gitignore-path (fs/path dir ".gitignore")
        pattern "cleanup-maid-*"
        gitignore-exists? (fs/exists? gitignore-path)]

    (if gitignore-exists?
      ;; Check if pattern already exists
      (let [content (slurp (str gitignore-path))]
        (if (str/includes? content pattern)
          (callout {:type :info} (bling [:green "✓"] " Pattern '" [:bold pattern] "' already exists in " [:bold (str gitignore-path)]))
          (do
            (spit (str gitignore-path) (str content "\n" pattern "\n"))
            (callout {:type :success} (bling [:green "✓"] " Added '" [:bold pattern] "' to " [:bold (str gitignore-path)])))))
      ;; Create new .gitignore file
      (do
        (spit (str gitignore-path) (str pattern "\n"))
        (callout {:type :success} (bling [:green "✓"] " Created " [:bold (str gitignore-path)] " with pattern '" [:bold pattern] "'"))))))

(defn gitignore-cleanup-files
  "Add cleanup-maid-* to .gitignore in the specified directory"
  [dir]
  (if (fs/exists? dir)
    (if (fs/directory? dir)
      (add-to-gitignore dir)
      (callout {:type :error} (bling [:red "Error:"] " Path is not a directory: " [:bold dir])))
    (callout {:type :error} (bling [:red "Error:"] " Directory does not exist: " [:bold dir]))))

(defn git-repository?
  "Check if the given directory is inside a Git repository by looking for .git directory"
  [dir]
  (fs/exists? (fs/path dir ".git")))

(defn show-git-hint
  "Show a hint about using --gitignore if we're in a Git repository"
  [dir]
  (when (git-repository? dir)
    (callout {:type :info}
             (bling [:cyan "💡 Tip:"] " You're in a Git repository! Use "
                    [:bold "--gitignore"] " to automatically add cleanup files to .gitignore:\n"
                    "       " [:bold "bb-maid clean-in 7d --gitignore"]))))

(defn confirm-deletion [dir date-str]
  (callout {:type :warning}
           (bling [:bold date-str] " || " [:bold (str dir)] " || Delete the directory " [:bold (last (str/split (str dir) #"/"))] " and everything in it? (y/N)"))
  (flush)
  (let [response (str/trim (read-line))]
    (= (str/lower-case response) "y")))

(defn parse-clean-options
  "Parse command-line options for the clean command.
   Returns a map with :path, :glob-opts, :dry-run, :auto-confirm, and :list keys."
  [args]
  (loop [remaining args
         result {:path nil
                 :glob-opts {:follow-links false
                             :max-depth Integer/MAX_VALUE}
                 :dry-run false
                 :auto-confirm false
                 :list false}]
    (if (empty? remaining)
      result
      (let [arg (first remaining)]
        (cond
          ;; Flags with values
          (= arg "--max-depth")
          (if-let [depth (second remaining)]
            (recur (drop 2 remaining)
                   (assoc-in result [:glob-opts :max-depth] (Integer/parseInt depth)))
            (do
              (callout {:type :error} (bling [:red "Error:"] " --max-depth requires a number"))
              result))

          ;; Boolean flags
          (= arg "--follow-links")
          (recur (rest remaining)
                 (assoc-in result [:glob-opts :follow-links] true))

          (or (= arg "--yes") (= arg "-y"))
          (recur (rest remaining)
                 (assoc result :auto-confirm true))

          (or (= arg "--dry-run") (= arg "-n"))
          (recur (rest remaining)
                 (assoc result :dry-run true))

          (= arg "--list")
          (recur (rest remaining)
                 (assoc result :list true))

          ;; Path argument (doesn't start with --)
          (not (str/starts-with? arg "--"))
          (recur (rest remaining)
                 (assoc result :path arg))

          ;; Unknown flag
          :else
          (do
            (callout {:type :warning} (bling [:yellow "Warning:"] " Unknown option: " [:bold arg]))
            (recur (rest remaining) result)))))))

(defn parse-clean-in-options
  "Parse command-line options for the clean-in command.
   Returns a map with :duration, :path, and :gitignore keys."
  [args]
  (loop [remaining args
         result {:duration nil
                 :path nil
                 :gitignore false}]
    (if (empty? remaining)
      result
      (let [arg (first remaining)]
        (cond
          ;; Boolean flags
          (= arg "--gitignore")
          (recur (rest remaining)
                 (assoc result :gitignore true))

          ;; Duration argument (ends with 'd')
          (and (re-matches #"\d+d" arg) (nil? (:duration result)))
          (recur (rest remaining)
                 (assoc result :duration arg))

          ;; Path argument (doesn't start with -- and duration is already set)
          (and (not (str/starts-with? arg "--")) (:duration result) (nil? (:path result)))
          (recur (rest remaining)
                 (assoc result :path arg))

          ;; First non-option argument is duration if not already set
          (and (not (str/starts-with? arg "--")) (nil? (:duration result)))
          (recur (rest remaining)
                 (assoc result :duration arg))

          ;; Unknown flag
          :else
          (do
            (callout {:type :warning} (bling [:yellow "Warning:"] " Unknown option: " [:bold arg]))
            (recur (rest remaining) result)))))))

(defn process-file-with-options [file opts]
  (let [parent-dir (fs/parent file)
        filename (fs/file-name file)
        formatted-filename (str filename)
        formatted-dir (str parent-dir)]
    (callout {:type :info} (bling "Processing file: " [:bold formatted-filename] " in directory: " [:bold formatted-dir]))
    (when-let [date-str (extract-date filename)]
      (when (past-date? date-str)
        (if (:dry-run opts)
          (callout {:type :warning} (bling [:yellow "Would delete:"] " " [:bold (str parent-dir)]))
          (when (or (:auto-confirm opts) (confirm-deletion parent-dir date-str))
            (println "Deleting directory: " parent-dir " with date: " date-str)
            (fs/delete-tree parent-dir)))))))

(defn days-until
  "Calculate days until the given date. Negative if past."
  [date-str]
  (let [file-date (java.time.LocalDate/parse date-str)
        current-date (java.time.LocalDate/now)
        days (.between java.time.temporal.ChronoUnit/DAYS current-date file-date)]
    days))

(defn format-date-display
  "Format date with days remaining and color coding"
  [date-str]
  (let [days (days-until date-str)]
    (cond
      (< days 0)
      (bling [:red date-str] [:red (str " (EXPIRED " (Math/abs days) " day" (if (= (Math/abs days) 1) "" "s") " ago)")])

      (<= days 7)
      (bling [:yellow date-str] [:yellow (str " (in " days " day" (if (= days 1) "" "s") ")")])

      :else
      (bling [:green date-str] [:green (str " (in " days " day" (if (= days 1) "" "s") ")")]))))

(defn list-cleanup-directories
  "List all directories with cleanup-maid files, sorted by date (earliest first)"
  [entrypoint opts]
  (let [glob-opts (:glob-opts opts {:follow-links false
                                    :max-depth Integer/MAX_VALUE})
        files (fs/glob entrypoint "**/*" glob-opts)
        cleanup-files (filter #(extract-date (fs/file-name %)) files)
        file-data (map (fn [file]
                         {:file file
                          :parent (fs/parent file)
                          :date-str (extract-date (fs/file-name file))
                          :date (java.time.LocalDate/parse
                                 (extract-date (fs/file-name file)))})
                       cleanup-files)
        sorted-data (sort-by :date file-data)]

    (if (empty? sorted-data)
      (callout {:type :info} (bling [:cyan "No cleanup files found in "] [:bold (str entrypoint)]))
      (do
        (callout {:type :info} (bling [:cyan "Found "] [:bold (str (count sorted-data))] [:cyan " cleanup file" (if (= (count sorted-data) 1) "" "s") ":"]))
        (println)
        (doseq [{:keys [date-str parent]} sorted-data]
          (println (format-date-display date-str) "   " (str parent)))))))

(defn search-and-delete
  "Recursively search for cleanup-maid files and delete expired directories.
   Options:
   - :glob-opts - options passed to fs/glob (:follow-links, :max-depth)
   - :dry-run - if true, only show what would be deleted
   - :auto-confirm - if true, skip confirmation prompts"
  [entrypoint opts]
  (let [glob-opts (:glob-opts opts {:follow-links false
                                    :max-depth Integer/MAX_VALUE})
        files (fs/glob entrypoint "**/*" glob-opts)
        expired-count (atom 0)
        symlink-count (atom 0)]
    (when (:dry-run opts)
      (callout {:type :info} (bling [:cyan "DRY RUN MODE:"] " No files will be deleted")))

    ;; Check for symlinks in the traversal and warn if not following them
    (when-not (get-in opts [:glob-opts :follow-links])
      (doseq [file files]
        (when (fs/sym-link? file)
          (swap! symlink-count inc)
          (callout {:type :warning} (bling [:yellow "Skipping symlink:"] " " [:bold (str file)] " (use --follow-links to traverse)")))))

    (doseq [file files]
      (when-let [date-str (extract-date (fs/file-name file))]
        (when (past-date? date-str)
          (swap! expired-count inc)
          (process-file-with-options file opts))))

    (when (and (pos? @expired-count) (:dry-run opts))
      (callout {:type :info} (bling [:cyan "Summary:"] " Found " [:bold (str @expired-count)] " expired director" (if (= @expired-count 1) "y" "ies"))))

    (when (and (pos? @symlink-count) (not (get-in opts [:glob-opts :follow-links])))
      (callout {:type :info} (bling [:cyan "Info:"] " Skipped " [:bold (str @symlink-count)] " symlink" (if (= @symlink-count 1) "" "s") " (use --follow-links to traverse)")))))

(defn -main [& args]
  (let [command (first args)
        remaining-args (rest args)]
    (cond
      (= command "clean")
      (let [opts (parse-clean-options remaining-args)
            dir (or (:path opts) ".")]
        (if (fs/exists? dir)
          (if (:list opts)
            (list-cleanup-directories dir opts)
            (search-and-delete dir opts))
          (callout {:type :error} (bling [:red "Error:"] " Directory does not exist: " [:bold dir]))))

      (= command "clean-in")
      (let [opts (parse-clean-in-options remaining-args)
            duration (:duration opts)
            path (or (:path opts) ".")]
        (cond
          (not duration)
          (callout {:type :error} (bling [:red "Error:"] " Please specify a duration (e.g., '7d' for 7 days)"))

          (not (fs/exists? path))
          (callout {:type :error} (bling [:red "Error:"] " Directory does not exist: " [:bold path]))

          :else
          (do
            (create-cleanup-file duration path)
            (if (:gitignore opts)
              (gitignore-cleanup-files path)
              (show-git-hint path)))))

      (= command "gitignore")
      (let [path (or (first remaining-args) ".")]
        (gitignore-cleanup-files path))

      :else
      (do
        (println "Usage:")
        (println "  bb-maid clean [directory] [options]")
        (println "    Clean up expired directories (defaults to current directory)")
        (println "    Options:")
        (println "      --max-depth <n>     Limit recursion depth (default: unlimited)")
        (println "      --follow-links      Follow symbolic links (default: false)")
        (println "      --yes, -y           Skip confirmation prompts")
        (println "      --dry-run, -n       Show what would be deleted without deleting")
        (println "      --list              List all cleanup files sorted by date (no deletion)")
        (println "")
        (println "  bb-maid clean-in <duration> [directory] [options]")
        (println "    Create a cleanup file (e.g., '7d' for 7 days, defaults to current directory)")
        (println "    Options:")
        (println "      --gitignore         Also add cleanup-maid-* pattern to .gitignore")
        (println "")
        (println "  bb-maid gitignore [directory]")
        (println "    Add cleanup-maid-* pattern to .gitignore (defaults to current directory)")))))
