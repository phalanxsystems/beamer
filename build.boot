(set-env!
  :resource-paths #{"src"}
  :target-path "target/"
  :dependencies '[[ring/ring-codec         "1.0.0"]])


(task-options!
  pom {:project 'rowtr/s3-beam
       :version "0.1.0"
       :description "library for client upload to s3"})

