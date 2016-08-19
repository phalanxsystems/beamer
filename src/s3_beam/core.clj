(ns s3-beam.core
  (:require [clojure.data.json :as json]
            [ring.util.codec :refer [base64-encode]])
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)
           (java.text SimpleDateFormat)
           (java.util Date TimeZone)))

(defn now-plus [n]
  "Returns current time plus `n` minutes as string"
  (let [f (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")]
    (.setTimeZone f (TimeZone/getTimeZone "UTC" ))
    (.format f (Date. (+ (System/currentTimeMillis) (* n 60 1000))))))

(defn policy
  "Generate policy for upload of `key` with `mime-type` to be uploaded
  within optional `expiration-window` (defaults to 60)."
  ([bucket key mime-type]
     (policy bucket key mime-type 60))
  ([bucket key mime-type expiration-window]
     (ring.util.codec/base64-encode
      (.getBytes (json/write-str { "expiration" (now-plus expiration-window)
                                   "conditions" [{"bucket" bucket}
                                                 {"acl" "public-read"}
                                                 ["starts-with" "$Content-Type" mime-type]
                                                 ["starts-with" "$key" key]
                                                 {"success_action_status" "201"}]})
                 "UTF-8"))))

(defn hmac-sha1 [key string]
  "Returns signature of `string` with a given `key` using SHA-1 HMAC."
  (ring.util.codec/base64-encode
   (.doFinal (doto (javax.crypto.Mac/getInstance "HmacSHA1")
               (.init (javax.crypto.spec.SecretKeySpec. (.getBytes key) "HmacSHA1")))
             (.getBytes string "UTF-8"))))

(defn sign-upload [{:keys [action file-name folder-name mime-type bucket aws-access-key aws-secret-key]}]
  (let [k         (str (when folder-name (str folder-name "/")) file-name)
        p         (policy bucket k mime-type)
        action    (str "//" (or action (str bucket ".s3.amazonaws.com/")))]
    {:action action
     :key   k
     :Content-Type mime-type
     :policy p
     :acl    "public-read"
     :success_action_status "201"
     :AWSAccessKeyId aws-access-key
     :signature (hmac-sha1 aws-secret-key p)}))
