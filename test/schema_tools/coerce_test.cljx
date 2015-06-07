(ns schema-tools.coerce-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
    #+cljs [cljs.test :as test :refer-macros [deftest testing is]]
            [schema.coerce :as sc]
            [clojure.string :as str]
            [schema-tools.coerce :as stc]))

(deftest forwarding-matcher-test
  (let [string->vec (fn [schema]
                      (if (vector? schema)
                        (fn [x]
                          (if (string? x)
                            (str/split x #",")
                            x))))
        string->long (fn [schema]
                       (if (= Long schema)
                         (fn [x]
                           (if (string? x)
                             (Long/parseLong x)
                             x))))
        string->vec->long (stc/forwarding-matcher string->vec string->long)
        string->long->vec (stc/forwarding-matcher string->long string->vec)]
    (testing "string->vec->long is able to parse both \"1,2,3\" and \"1\"."
      (is (= ((sc/coercer {:a [Long]
                           :b [Long]
                           :c [[Long]]
                           :d Long
                           :e Long}
                          string->vec->long)
               {:a "1,2,3"
                :b ["1" "2" "3"]
                :c ["1,2,3" "4,5,6" "7,8,9"]
                :d 1
                :e "1"})
             {:a [1 2 3]
              :b [1 2 3]
              :c [[1 2 3] [4 5 6] [7 8 9]]
              :d 1
              :e 1})))
    (testing "string->vec->long is able to parse both \"1,2,3\" and \"1\"."
      (is (= ((sc/coercer {:a [Long]
                           :b [Long]
                           :c [[Long]]
                           :d Long
                           :e Long}
                          string->long->vec)
               {:a "1,2,3"
                :b ["1" "2" "3"]
                :c ["1,2,3" "4,5,6" "7,8,9"]
                :d 1
                :e "1"})
             {:a [1 2 3]
              :b [1 2 3]
              :c [[1 2 3] [4 5 6] [7 8 9]]
              :d 1
              :e 1})))))

(deftest or-matcher-test
  (let [base-matcher (fn [schema-pred value-pred value-fn]
                       (fn [schema]
                         (if (schema-pred schema)
                           (fn [x]
                             (if (value-pred x)
                               (value-fn x))))))
        m1 (base-matcher #(= String %) string? #(.toUpperCase %))
        m2 (base-matcher #(= Long %) number? inc)
        m3 (base-matcher #(= Boolean %) (partial instance? Boolean) not)
        m4 (base-matcher #(= String %) string? #(.toLowerCase %))
        m1-or-m2-or-m3-or-m4 (stc/or-matcher m1 m2 m3 m4)]
    (testing "or-matcher selects first matcher where schema matches"
      (is (= ((sc/coercer {:band String, :number Long, :lucid Boolean} m1-or-m2-or-m3-or-m4)
               {:band "kiss", :number 41, :lucid false}) {:band "KISS", :number 42, :lucid true})))))
