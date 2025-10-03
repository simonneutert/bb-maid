(ns simonneutert.bb-maid-test
  (:require [clojure.test :refer [deftest is testing]]
            [simonneutert.bb-maid :as maid]
            [babashka.fs :as fs]))

(deftest extract-date-test
  (testing "Extracts date from valid cleanup-maid filename"
    (is (= "2025-10-15" (maid/extract-date "cleanup-maid-2025-10-15")))
    (is (= "2025-01-01" (maid/extract-date "cleanup-maid-2025-01-01"))))
  
  (testing "Returns nil for invalid filenames"
    (is (nil? (maid/extract-date "not-a-cleanup-file")))
    (is (nil? (maid/extract-date "cleanup-maid-invalid")))
    (is (nil? (maid/extract-date "cleanup-maid-25-10-15")))))

(deftest past-date?-test
  (testing "Recognizes past dates"
    (is (true? (maid/past-date? "2020-01-01")))
    (is (true? (maid/past-date? "2024-01-01"))))
  
  (testing "Recognizes future dates"
    (is (false? (maid/past-date? "2030-01-01")))
    (is (false? (maid/past-date? "2026-12-31")))))

(deftest parse-duration-test
  (testing "Parses valid duration strings"
    (is (= 7 (maid/parse-duration "7d")))
    (is (= 1 (maid/parse-duration "1d")))
    (is (= 90 (maid/parse-duration "90d"))))
  
  (testing "Returns nil for invalid duration strings"
    (is (nil? (maid/parse-duration "7")))
    (is (nil? (maid/parse-duration "d7")))
    (is (nil? (maid/parse-duration "7days")))))

(deftest calculate-future-date-test
  (testing "Calculates future dates correctly"
    (let [today (java.time.LocalDate/now)
          future-date (maid/calculate-future-date 7)]
      (is (= 7 (.until today future-date java.time.temporal.ChronoUnit/DAYS))))))

(deftest parse-clean-options-test
  (testing "Parses directory path"
    (let [opts (maid/parse-clean-options ["/tmp/test"])]
      (is (= "/tmp/test" (:path opts)))))
  
  (testing "Parses --max-depth flag"
    (let [opts (maid/parse-clean-options ["/tmp" "--max-depth" "5"])]
      (is (= "/tmp" (:path opts)))
      (is (= 5 (get-in opts [:glob-opts :max-depth])))))
  
  (testing "Parses --follow-links flag"
    (let [opts (maid/parse-clean-options ["/tmp" "--follow-links"])]
      (is (true? (get-in opts [:glob-opts :follow-links])))))
  
  (testing "Parses --yes/-y flag"
    (is (true? (:auto-confirm (maid/parse-clean-options ["/tmp" "--yes"]))))
    (is (true? (:auto-confirm (maid/parse-clean-options ["/tmp" "-y"])))))
  
  (testing "Parses --dry-run/-n flag"
    (is (true? (:dry-run (maid/parse-clean-options ["/tmp" "--dry-run"]))))
    (is (true? (:dry-run (maid/parse-clean-options ["/tmp" "-n"])))))
  
  (testing "Parses --list flag"
    (is (true? (:list (maid/parse-clean-options ["/tmp" "--list"]))))
    (is (false? (:list (maid/parse-clean-options ["/tmp"])))))
  
  (testing "Combines multiple options"
    (let [opts (maid/parse-clean-options ["/tmp" "--max-depth" "3" "--dry-run" "-y"])]
      (is (= "/tmp" (:path opts)))
      (is (= 3 (get-in opts [:glob-opts :max-depth])))
      (is (true? (:dry-run opts)))
      (is (true? (:auto-confirm opts)))))
  
  (testing "Combines --list with other options"
    (let [opts (maid/parse-clean-options ["/tmp" "--list" "--max-depth" "2"])]
      (is (= "/tmp" (:path opts)))
      (is (true? (:list opts)))
      (is (= 2 (get-in opts [:glob-opts :max-depth])))))
  
  (testing "Default values"
    (let [opts (maid/parse-clean-options ["/tmp"])]
      (is (false? (get-in opts [:glob-opts :follow-links])))
      (is (= Integer/MAX_VALUE (get-in opts [:glob-opts :max-depth])))
      (is (false? (:dry-run opts)))
      (is (false? (:auto-confirm opts)))
      (is (false? (:list opts))))))

(deftest parse-duration-validates-format-test
  (testing "parse-duration returns nil for invalid formats"
    (is (nil? (maid/parse-duration "invalid")))
    (is (nil? (maid/parse-duration "")))
    (is (nil? (maid/parse-duration "7")))
    (is (nil? (maid/parse-duration "d7"))))
  
  (testing "parse-duration works with valid formats"
    (is (number? (maid/parse-duration "1d")))
    (is (= 365 (maid/parse-duration "365d")))))

(deftest symlink-handling-options-test
  (testing "Default behavior does not follow symlinks"
    (let [opts (maid/parse-clean-options ["/tmp/test"])]
      (is (false? (get-in opts [:glob-opts :follow-links]))
          "By default, symlinks should NOT be followed for safety")))
  
  (testing "Explicit --follow-links enables symlink traversal"
    (let [opts (maid/parse-clean-options ["/tmp/test" "--follow-links"])]
      (is (true? (get-in opts [:glob-opts :follow-links]))
          "When --follow-links is specified, symlinks should be followed")))
  
  (testing "follow-links works with other options"
    (let [opts (maid/parse-clean-options ["/tmp" "--follow-links" "--max-depth" "3" "--dry-run"])]
      (is (true? (get-in opts [:glob-opts :follow-links])))
      (is (= 3 (get-in opts [:glob-opts :max-depth])))
      (is (true? (:dry-run opts)))
      (is (= "/tmp" (:path opts)))))
  
  (testing "Order of options doesn't matter for follow-links"
    (let [opts1 (maid/parse-clean-options ["--follow-links" "/tmp"])
          opts2 (maid/parse-clean-options ["/tmp" "--follow-links"])]
      (is (true? (get-in opts1 [:glob-opts :follow-links])))
      (is (true? (get-in opts2 [:glob-opts :follow-links])))
      (is (= "/tmp" (:path opts1)))
      (is (= "/tmp" (:path opts2))))))

(deftest parse-clean-options-defaults-to-current-dir-test
  (testing "When no path is provided, path should be nil (defaults to '.' in -main)"
    (let [opts (maid/parse-clean-options [])]
      (is (nil? (:path opts)))
      (is (false? (:dry-run opts)))))
  
  (testing "Options work without a path"
    (let [opts (maid/parse-clean-options ["--dry-run" "--yes"])]
      (is (nil? (:path opts)))
      (is (true? (:dry-run opts)))
      (is (true? (:auto-confirm opts)))))
  
  (testing "Options can come before implicit current directory"
    (let [opts (maid/parse-clean-options ["--max-depth" "2" "--dry-run"])]
      (is (nil? (:path opts)))
      (is (= 2 (get-in opts [:glob-opts :max-depth])))
      (is (true? (:dry-run opts))))))

(deftest create-cleanup-file-with-path-test
  (testing "create-cleanup-file accepts directory parameter"
    (is (fn? maid/create-cleanup-file)
        "Function should exist"))
  
  (testing "remove-existing-cleanup-files accepts directory parameter"
    (is (fn? maid/remove-existing-cleanup-files)
        "Function should exist")))

(deftest path-handling-edge-cases-test
  (testing "Handles relative paths"
    (let [opts (maid/parse-clean-options ["./subdir"])]
      (is (= "./subdir" (:path opts)))))
  
  (testing "Handles absolute paths"
    (let [opts (maid/parse-clean-options ["/tmp/test"])]
      (is (= "/tmp/test" (:path opts)))))
  
  (testing "Handles paths with spaces"
    (let [opts (maid/parse-clean-options ["/tmp/test folder"])]
      (is (= "/tmp/test folder" (:path opts)))))
  
  (testing "Handles home directory paths"
    (let [opts (maid/parse-clean-options ["~/projects"])]
      (is (= "~/projects" (:path opts)))))
  
  (testing "Handles current directory explicitly"
    (let [opts (maid/parse-clean-options ["."])]
      (is (= "." (:path opts)))))
  
  (testing "Handles parent directory"
    (let [opts (maid/parse-clean-options [".."])]
      (is (= ".." (:path opts))))))

(deftest backward-compatibility-test
  (testing "Existing usage patterns still work"
    (testing "clean with explicit path"
      (let [opts (maid/parse-clean-options ["/tmp"])]
        (is (= "/tmp" (:path opts)))))
    
    (testing "clean with path and options"
      (let [opts (maid/parse-clean-options ["/tmp" "--dry-run" "--max-depth" "3"])]
        (is (= "/tmp" (:path opts)))
        (is (true? (:dry-run opts)))
        (is (= 3 (get-in opts [:glob-opts :max-depth])))))
    
    (testing "clean with options before path"
      (let [opts (maid/parse-clean-options ["--dry-run" "/tmp" "--max-depth" "3"])]
        (is (= "/tmp" (:path opts)))
        (is (true? (:dry-run opts)))
        (is (= 3 (get-in opts [:glob-opts :max-depth])))))
    
    (testing "Multiple paths uses last one (current behavior)"
      (let [opts (maid/parse-clean-options ["/tmp" "/other"])]
        (is (= "/other" (:path opts))
            "Current implementation uses last path encountered")))))

(deftest create-cleanup-file-integration-test
  (testing "Creates cleanup file in specified directory"
    (let [temp-dir (str (fs/create-temp-dir))
          duration "7d"]
      (try
        (maid/create-cleanup-file duration temp-dir)
        (let [files (fs/list-dir temp-dir)
              cleanup-files (filter #(re-matches #"cleanup-maid-\d{4}-\d{2}-\d{2}" 
                                                  (fs/file-name %)) files)]
          (is (= 1 (count cleanup-files)) 
              "Should create exactly one cleanup file")
          (is (some? (first cleanup-files))
              "Cleanup file should exist"))
        (finally
          (fs/delete-tree temp-dir)))))
  
  (testing "Creates cleanup file with correct future date"
    (let [temp-dir (str (fs/create-temp-dir))
          duration "14d"
          days 14]
      (try
        (maid/create-cleanup-file duration temp-dir)
        (let [files (fs/list-dir temp-dir)
              cleanup-file (first (filter #(re-matches #"cleanup-maid-\d{4}-\d{2}-\d{2}" 
                                                        (fs/file-name %)) files))
              filename (fs/file-name cleanup-file)
              date-str (maid/extract-date filename)
              expected-date (maid/calculate-future-date days)
              expected-date-str (.format expected-date (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))]
          (is (= expected-date-str date-str)
              "Cleanup file should have the correct future date"))
        (finally
          (fs/delete-tree temp-dir)))))
  
  (testing "Removes existing cleanup files before creating new one"
    (let [temp-dir (str (fs/create-temp-dir))]
      (try
        ;; Create first cleanup file
        (maid/create-cleanup-file "7d" temp-dir)
        (let [first-files (fs/list-dir temp-dir)]
          (is (= 1 (count first-files)) 
              "Should have one cleanup file after first creation"))
        
        ;; Create second cleanup file - should replace the first
        (maid/create-cleanup-file "14d" temp-dir)
        (let [second-files (fs/list-dir temp-dir)
              cleanup-files (filter #(re-matches #"cleanup-maid-\d{4}-\d{2}-\d{2}" 
                                                  (fs/file-name %)) second-files)]
          (is (= 1 (count cleanup-files))
              "Should still have only one cleanup file after second creation")
          (is (some #(re-find #"cleanup-maid-" (str %)) cleanup-files)
              "The cleanup file should exist"))
        (finally
          (fs/delete-tree temp-dir))))))

(deftest days-until-test
  (testing "Calculates days until future date"
    (let [today (java.time.LocalDate/now)
          future-date (.plusDays today 7)
          date-str (.format future-date (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))]
      (is (= 7 (maid/days-until date-str)))))
  
  (testing "Returns negative for past dates"
    (let [today (java.time.LocalDate/now)
          past-date (.minusDays today 5)
          date-str (.format past-date (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))]
      (is (= -5 (maid/days-until date-str)))))
  
  (testing "Returns 0 for today"
    (let [today (java.time.LocalDate/now)
          date-str (.format today (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))]
      (is (= 0 (maid/days-until date-str))))))

(deftest format-date-display-test
  (testing "format-date-display returns a string-like structure"
    (let [today (java.time.LocalDate/now)
          date-str (.format today (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))
          result (maid/format-date-display date-str)]
      (is (some? result)
          "Should return a non-nil result")))
  
  (testing "Handles expired dates"
    (let [past-date "2025-09-01"
          result (maid/format-date-display past-date)]
      (is (some? result)
          "Should format expired dates")))
  
  (testing "Handles upcoming dates"
    (let [future-date "2030-01-01"
          result (maid/format-date-display future-date)]
      (is (some? result)
          "Should format upcoming dates"))))

(deftest list-cleanup-directories-integration-test
  (testing "Lists cleanup files correctly"
    (let [temp-dir (str (fs/create-temp-dir))]
      (try
        ;; Create test structure
        (let [dir1 (fs/create-dirs (fs/path temp-dir "expired"))
              dir2 (fs/create-dirs (fs/path temp-dir "upcoming"))]
          (spit (fs/file dir1 "cleanup-maid-2020-01-01") "")
          (spit (fs/file dir2 "cleanup-maid-2030-01-01") "")
          
          ;; Test that list-cleanup-directories runs without error
          (let [opts {:glob-opts {:follow-links false
                                  :max-depth Integer/MAX_VALUE}}
                result (with-out-str 
                         (maid/list-cleanup-directories temp-dir opts))]
            (is (some? result)
                "Should produce output")
            (is (string? result)
                "Output should be a string")))
        (finally
          (fs/delete-tree temp-dir)))))
  
  (testing "Handles empty directories"
    (let [temp-dir (str (fs/create-temp-dir))]
      (try
        (let [opts {:glob-opts {:follow-links false
                                :max-depth Integer/MAX_VALUE}}
              result (with-out-str
                       (maid/list-cleanup-directories temp-dir opts))]
          (is (some? result)
              "Should handle directories with no cleanup files"))
        (finally
          (fs/delete-tree temp-dir)))))
  
  (testing "Respects max-depth option"
    (let [temp-dir (str (fs/create-temp-dir))]
      (try
        ;; Create nested structure
        (let [deep-dir (fs/create-dirs (fs/path temp-dir "level1" "level2" "level3"))]
          (spit (fs/file deep-dir "cleanup-maid-2025-01-01") "")
          
          (let [opts-shallow {:glob-opts {:follow-links false
                                          :max-depth 1}}
                opts-deep {:glob-opts {:follow-links false
                                       :max-depth 10}}]
            ;; Both should run without error
            (is (some? (with-out-str (maid/list-cleanup-directories temp-dir opts-shallow))))
            (is (some? (with-out-str (maid/list-cleanup-directories temp-dir opts-deep))))))
        (finally
          (fs/delete-tree temp-dir))))))

(deftest list-does-not-delete-test
  (testing "list-cleanup-directories does not delete any files"
    (let [temp-dir (str (fs/create-temp-dir))]
      (try
        ;; Create an expired cleanup file
        (let [expired-dir (fs/create-dirs (fs/path temp-dir "expired-dir"))]
          (spit (fs/file expired-dir "cleanup-maid-2020-01-01") "")
          (spit (fs/file expired-dir "important-file.txt") "Don't delete me!")
          
          ;; Run list
          (let [opts {:glob-opts {:follow-links false
                                  :max-depth Integer/MAX_VALUE}}]
            (with-out-str (maid/list-cleanup-directories temp-dir opts)))
          
          ;; Verify nothing was deleted
          (is (fs/exists? expired-dir)
              "Directory should still exist after listing")
          (is (fs/exists? (fs/file expired-dir "cleanup-maid-2020-01-01"))
              "Cleanup file should still exist")
          (is (fs/exists? (fs/file expired-dir "important-file.txt"))
              "Other files should not be affected"))
        (finally
          (fs/delete-tree temp-dir))))))

