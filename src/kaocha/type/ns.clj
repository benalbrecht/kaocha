(ns kaocha.type.ns
  (:require [clojure.test :as t]
            [kaocha.core-ext :refer :all]
            [kaocha.testable :as testable]
            [clojure.spec.alpha :as s]))

(defn ->testable [ns-name]
  {:kaocha.testable/type :kaocha.type/ns
   :kaocha.testable/id   (keyword (str ns-name))
   :kaocha.ns/name       ns-name})

(defmethod testable/-load :kaocha.type/ns [testable]
  ;; TODO If the namespace has a test-ns-hook function, call that:
  ;; if-let [v (find-var (symbol (:kaocha.ns/name testable) "test-ns-hook"))]

  (let [ns-name (:kaocha.ns/name testable)]
    (try
      (require ns-name)
      (let [ns-obj (the-ns ns-name)]
        (->> ns-obj
             ns-publics
             (filter (comp :test meta val))
             (map (fn [[sym var]]
                    (let [nsname    (:kaocha.ns/name testable)
                          test-name (symbol (str nsname) (str sym))]
                      {:kaocha.testable/type :kaocha.type/var
                       :kaocha.testable/id   (keyword test-name)
                       :kaocha.var/name      test-name
                       :kaocha.var/var       var
                       :kaocha.var/test      (:test (meta var))})))
             (assoc testable
                    :kaocha.ns/ns ns-obj
                    :kaocha.test-plan/tests)))
      (catch Throwable t
        (assoc testable
               :kaocha.test-plan/load-error t)))))

(defmethod testable/-run :kaocha.type/ns [testable]
  (binding [t/*report-counters* (ref t/*initial-report-counters*)]
    (t/do-report {:type :begin-test-ns, :ns (:kaocha.ns/ns testable)})
    (let [tests    (-> testable
                       :kaocha.test-plan/tests
                       testable/run-testables)
          result (assoc (dissoc testable :kaocha.test-plan/tests)
                        :kaocha.result/tests
                        tests)]
      (t/do-report {:type :end-test-ns, :ns (:kaocha.ns/ns testable)})
      result)))

(s/def :kaocha.type/ns (s/keys :req [:kaocha.testable/type
                                     :kaocha.testable/id
                                     :kaocha.ns/name]
                               :opt [:kaocha.ns/ns
                                     :kaocha.test-plan/tests]))

(s/def :kaocha.ns/name simple-symbol?)
(s/def :kaocha.ns/ns   ns?)