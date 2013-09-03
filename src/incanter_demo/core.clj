(ns ^{:doc "Demonstration of Incanter datasets and charts."}
  incanter-demo.core
  (:use (incanter core io stats charts datasets optimize mongodb pdf)
        (somnium congomongo)))

; view a function
(view (function-plot sin -4 4))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Datasets
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; Dataset creation
;;

; create a small dataset
(def data
  (dataset ["x1" "x2" "x3"]
           [[1 2 3]
            [4 5 6]
            [7 8 9]]))

; convert a sequence of maps
(to-dataset [{"x1" 1, "x2" 2, "x3" 3} {"x1" 4, "x2" 5, "x3" 6}
             {"x1" 7, "x2" 8, "x3" 9}])

; convert a sequence of sequences
(to-dataset [[1 2 3]
              [4 5 6]
              [7 8 9]])

; conj columns together
(conj-cols [1 4 7] [2 5 8] [3 6 9])

; conj rows together
(conj-rows [1 2 3] [4 5 6] [7 8 9])

;;
;; Saving data
;;

; save a dataset in a CSV file
(save data "data/data.csv")

; save a dataset in a MongoDB database
(with-data (get-dataset :cars)
  (mongo! :db "mydb")
  (insert-dataset :cars $data))

;;
;; Reading datasets
;;

; read a built-in sample dataset
(get-dataset :cars)

(with-data (get-dataset :cars)
  (save $data "data/cars.csv" :delim \, :header ["dist" "speed"])
  (save $data "data/cars.tdd" :delim \tab :header ["dist" "speed"]))

; read a comma-delimited file
(read-dataset "./data/cars.csv"
              :header true)

; read a tab-delimited file
(read-dataset "./data/cars.tdd"
               :header true
               :delim \tab)

;
; read a comma-delimited file from a URL
(read-dataset "http://bit.ly/aZyjKa"
              :header true)


; Read data from mongodb
(mongo! :db "mydb")
(view (fetch-dataset :cars))

;;
;; Column selection
;;

; select a single column
($ :speed (get-dataset :cars))

; use with-data macro to bind dataset
(with-data (get-dataset :cars)
   [(mean ($ :speed))
    (sd ($ :speed))])

(with-data (get-dataset :iris)
  ; view dataset bound to $data
  (view $data)
  ; select multiple columns
  (view ($ [:Sepal.Length :Sepal.Width :Species]))
  ; exclude multiple columns
  (view ($ [:not :Petal.Width :Petal.Length]))
  ; select only the first row
  (view ($ 0 [:not :Petal.Width :Petal.Length])))

;;
;; Row selection
;;

; select rows where species equals ‘setosa’
($where {:Species "setosa"}
              (get-dataset :iris))

; select rows where petal-width < 1.5
($where {:Petal.Width {:lt 1.5}}
        (get-dataset :iris))

; select rows where 1.0 < petal-width < 1.5
($where {:Petal.Width {:gt 1.0, :lt 1.5}}
        (get-dataset :iris))

; select rows where species is ‘virginica’ or ‘setosa’
($where {:Petal.Width {:gt 1.0, :lt 1.5}
         :Species {:in #{"virginica" "setosa"}}}
        (get-dataset :iris))

; select rows using an arbitrary predicate function
($where (fn [row]
           (or (< (row :Petal.Width) 1.0)
               (> (row :Petal.Length) 5.0)))
        (get-dataset :iris))

;;
;; Sorting data
;;

(with-data (get-dataset :hair-eye-color)
  (view $data)
  ;; Sort by count in descending order
  (view ($order :count :desc))
  ;; Sort by hair and then eye colour
  (view ($order [:hair :eye] :desc)))

;;
;; Rolling up data
;;

; mean petal-length by species
(->> (get-dataset :iris) ; Note the 'thread last' macro!
     ($rollup mean :Petal.Length :Species)
     view)

; standard error of petal-length
(->> (get-dataset :iris)
     ($rollup #(/ (sd %) (count %))
              :Petal.Length :Species)
     view)

; sum of people with each hair/eye color combination,
; sorted from most to least common
(->> (get-dataset :hair-eye-color)
     ($rollup sum :count [:hair :eye])
     ($order :count :desc)
     view)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Charts
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; Scatter plots
;;

; plot ‘sepal length’ vs ‘sepal width’
(view (scatter-plot :Sepal.Length :Sepal.Width
                    :data (get-dataset :iris)))

; group by species values
(view (scatter-plot :Sepal.Length :Sepal.Width
                     :data (get-dataset :iris)
                     :group-by :Species))

;;
;; Chart options
;;

; change chart title & axes labels
(view (scatter-plot :Sepal.Length :Sepal.Width
                    :data (get-dataset :iris)
                    :group-by :Species
                    :title "Fisher Iris Data"
                    :x-label "Sepal Length (cm)"
                    :y-label "Sepal Width (cm)"))

;;
;; Saving charts
;;

; save chart to a PNG file
(save (scatter-plot :Sepal.Length :Sepal.Width :data (get-dataset :iris)
                    :group-by :Species
                    :title "Fisher Iris Data"
                    :x-label "Sepal Length (cm)"
                    :y-label "Sepal Width (cm)")
      "./iris-plot.png")

; save chart to an OutputStream
(def output-stream (java.io.ByteArrayOutputStream.))
(save (scatter-plot :Sepal.Length :Sepal.Width
                    :data (get-dataset :iris)
                    :group-by :Species
                    :title "Fisher Iris Data"
                    :x-label "Sepal Length (cm)"
                    :y-label "Sepal Width (cm)")
      output-stream)

; save chart to a PDF file
(save-pdf (scatter-plot :Sepal.Length :Sepal.Width
                          :data (get-dataset :iris)
                          :group-by :Species
                          :title "Fisher Iris Data"
                          :x-label "Sepal Length (cm)"
                          :y-label "Sepal Width (cm)")
          "./iris-plot.pdf")

;;
;; Adding data
;;

; plot points where petal-length <= 2 & petal-width < .75
(with-data (get-dataset :iris)
    (doto (scatter-plot :Petal.Width :Petal.Length
                        :data ($where {:Petal.Length {:lte 2.0}
                                       :Petal.Width {:lt 0.75}}))
      view))

; add points where petal-length > 2 & petal-width >= .75
(with-data (get-dataset :iris)
    (doto (scatter-plot :Petal.Width :Petal.Length
                        :data ($where {:Petal.Length {:lte 2.0}
                                       :Petal.Width {:lt 0.75}}))
          (add-points :Petal.Width :Petal.Length
                      :data ($where {:Petal.Length {:gt 2.0}
                                     :Petal.Width {:gte 0.75}}))
          view))

; add a regression line
(with-data (get-dataset :iris)
  (let [lm (linear-model ($ :Petal.Length) ($ :Petal.Width))]
    (doto (scatter-plot :Petal.Width :Petal.Length
                        :data ($where {:Petal.Length {:lte 2.0}
                                       :Petal.Width {:lt 0.75}}))
      (add-points :Petal.Width :Petal.Length
                  :data ($where {:Petal.Length {:gt 2.0}
                                 :Petal.Width {:gte 0.75}}))
      (add-lines :Petal.Width (:fitted lm))
      view)))

;;
;; Bar & line charts
;;

; bar-chart of mean sepal-length for each species
(with-data ($rollup mean :Sepal.Length :Species
                    (get-dataset :iris))
  (view (bar-chart :Species :Sepal.Length)))

; line-chart of mean sepal-length for each species
(with-data ($rollup mean :Sepal.Length :Species
                    (get-dataset :iris))
  (view (line-chart :Species :Sepal.Length)))

; rollup the :count column using mean
(with-data ($rollup :mean :count [:hair :eye]
                    (get-dataset :hair-eye-color))
  (view $data)
  (view (bar-chart :hair :count
                   :group-by :eye
                   :legend true)))

; line-chart grouped by eye color
(with-data ($rollup :mean :count [:hair :eye]
                    (get-dataset :hair-eye-color))
  (view $data)
  (view (line-chart :hair :count
                    :group-by :eye
                    :legend true)))

; sort by sum of :count column
(with-data (->>  (get-dataset :hair-eye-color)
                 ($where {:hair {:in #{"brown" "blond"}}})
                 ($rollup :sum :count [:hair :eye])
                 ($order :count :desc))
  (view $data)
  (view (bar-chart :hair :count
                   :group-by :eye
                   :legend true)))

; bar-chart grouped by eye color
(with-data (->>  (get-dataset :hair-eye-color)
                 ($where {:hair {:in #{"brown" "blond"}}})
                 ($rollup :sum :count [:hair :eye])
                 ($order :count :desc))
  (view $data)
  (view (bar-chart :hair :count
                   :group-by :eye
                   :legend true)))

; line-chart grouped by eye color
(with-data (->>  (get-dataset :hair-eye-color)
                 ($where {:hair {:in #{"brown" "blond"}}})
                 ($rollup :sum :count [:hair :eye])
                 ($order :count :desc))
  (view $data)
  (view (line-chart :hair :count
                    :group-by :eye
                    :legend true)))

;;
;; XY & function plots
;;

; xy-plot of two continuous variables
(with-data (get-dataset :cars)
  (view (xy-plot :speed :dist)))

; function-plot of x3+2x2+2x+3
(use '(incanter core charts optimize))
(defn cubic [x] (+ (* x x x) (* 2 x x) (* 2 x) 3))
(doto (function-plot cubic -10 10)
  view)

; add the derivative of the function
(defn cubic [x] (+ (* x x x) (* 2 x x) (* 2 x) 3))
(doto (function-plot cubic -10 10)
  (add-function (derivative cubic) -10 10)
  view)

; add a sine wave
(defn cubic [x] (+ (* x x x) (* 2 x x) (* 2 x) 3))
(doto
    (function-plot cubic -10 10)
  (add-function (derivative cubic) -10 10)
  (add-function #(* 1000 (sin %)) -10 10)
  view)

;;
;; Histograms & box-plots
;;

; plot a sample from a gamma distribution
(doto
    (histogram (sample-gamma 1000)
               :density true
               :nbins 30)
  view)

; add a gamma pdf line
(doto (histogram (sample-gamma 1000)
                 :density true
                 :nbins 30)
  (add-function pdf-gamma 0 8)
  view)

; box-plots of petal-width grouped by species
(with-data (get-dataset :iris)
  (view (box-plot :Petal.Width
                  :group-by :Species)))

;;
;; Annotating charts
;;

; plot sin wave
(doto
    (function-plot sin -10 10)
  (add-text 0 0 "text at (0,0)")
  (add-pointer (- Math/PI) (sin (- Math/PI))
               :text "pointer at (sin -pi)")
  (add-pointer Math/PI (sin Math/PI)
               :text "pointer at(sin pi)"
               :angle :ne)
  (add-pointer (* 1/2 Math/PI) (sin (* 1/2 Math/PI))
               :text "pointer at(sin pi/2)"
               :angle :south)
  view)
