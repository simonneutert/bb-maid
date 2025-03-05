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
(let [entrypoint (first *command-line-args*)]
  (if entrypoint
    (search-and-delete entrypoint)
    (println "Usage: script.clj <entry-directory>")))
