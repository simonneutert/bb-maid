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
  
  (testing "Combines multiple options"
    (let [opts (maid/parse-clean-options ["/tmp" "--max-depth" "3" "--dry-run" "-y"])]
      (is (= "/tmp" (:path opts)))
      (is (= 3 (get-in opts [:glob-opts :max-depth])))
      (is (true? (:dry-run opts)))
      (is (true? (:auto-confirm opts)))))
  
  (testing "Default values"
    (let [opts (maid/parse-clean-options ["/tmp"])]
      (is (false? (get-in opts [:glob-opts :follow-links])))
      (is (= Integer/MAX_VALUE (get-in opts [:glob-opts :max-depth])))
      (is (false? (:dry-run opts)))
      (is (false? (:auto-confirm opts))))))

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
