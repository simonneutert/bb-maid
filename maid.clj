#!/usr/bin/env bb

(require '[clojure.java.io :as io])
(require '[clojure.string :as str])
(require '[babashka.fs :as fs])
(require '[bling.core :refer [bling callout point-of-interest]])

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
  "Remove any existing cleanup-maid files in the current directory"
  []
  (let [existing-files (filter #(re-matches #"cleanup-maid-\d{4}-\d{2}-\d{2}" (fs/file-name %))
                               (fs/list-dir "."))]
    (doseq [file existing-files]
      (callout {:type :warning} (bling "Removing existing cleanup file: " [:bold (fs/file-name file)]))
      (fs/delete file))))

(defn create-cleanup-file 
  "Create a cleanup file with a date based on the duration"
  [duration-str]
  (if-let [days (parse-duration duration-str)]
    (do
      (remove-existing-cleanup-files)
      (let [future-date (calculate-future-date days)
            date-str (.format future-date (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))
            filename (str "cleanup-maid-" date-str)]
        (callout {:type :info} (bling "Creating cleanup file: " [:bold filename] " (expires in " [:bold (str days)] " days)"))
        (spit filename "")
        (callout {:type :success} (bling [:green "✓"] " File created successfully"))))
    (callout {:type :error} (bling [:red "Error:"] " Invalid duration format. Use format like '7d' for 7 days"))))

(defn confirm-deletion [dir date-str]
   (callout {:type :warning}
           (bling [:bold date-str] " || " [:bold (str dir)] " || Delete the directory " [:bold (last (str/split (str dir) #"/"))] " and everything in it? (y/N)"))
  (flush)
  (let [response (str/trim (read-line))]
    (= (str/lower-case response) "y")))

(defn process-file [file]
  (let [parent-dir (fs/parent file)
        filename (fs/file-name file)
        formatted-filename (str filename)
        formatted-dir (str parent-dir)] 
    (callout {:type :info} (bling "Processing file: " [:bold formatted-filename] " in directory: " [:bold formatted-dir]))
    (when-let [date-str (extract-date filename)]
      (when (past-date? date-str)
        (when (confirm-deletion parent-dir date-str)
          (println "Deleting directory: " parent-dir " with date: " date-str)
          (fs/delete-tree parent-dir))))))

(defn search-and-delete [entrypoint]
  (doseq [file (fs/glob entrypoint "**/*")]
    (process-file file)))

;; Entry point
(let [command (first *command-line-args*)
      arg (second *command-line-args*)]
  (cond
    (= command "clean-in")
    (if arg
      (create-cleanup-file arg)
      (println "Usage: maid.clj clean-in <duration> (e.g., '7d' for 7 days)"))
    
    command
    (search-and-delete command)
    
    :else
    (println "Usage:\n  maid.clj <entry-directory>  - Clean up expired directories\n  maid.clj clean-in <duration> - Create a cleanup file (e.g., '7d' for 7 days)")))
