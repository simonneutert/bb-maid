(ns simonneutert.bb-maid-test
  (:require [clojure.test :refer [deftest is testing]]
            [simonneutert.bb-maid :as maid]))

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
