(ns zelkova-om-searcher.jsonp
  (:require [cljs.core.async :as async :refer [>! <!]]
            [jamesmacaulay.zelkova.signal :as z]
            [goog.net.Jsonp]
            [jamesmacaulay.zelkova.time :as time])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn pipeline
  [from to]
  (go-loop []
    (let [{:keys [url params] :as req} (<! from)]
      (if (nil? req)
        (async/close! to)
        (let [cb (fn [data]
                   (-> data
                       (js->clj :keywordize-keys true)
                       (with-meta {:request req})
                       (->> (async/put! to))))]
          (.send (goog.net.Jsonp. url) (clj->js params) cb cb)
          (recur))))))

(defn fetch-responses
  [requests]
  (z/splice pipeline
            (constantly {})
            requests))













;(defn dummy-response
;  [request]
;  (let [search-text (-> request :params :search)]
;    (with-meta
;      [(-> request)
;       (mapv (partial str search-text " ")
;             (range 10))]
;      {:request request})))
;
;(defn fetch-fake-responses
;  [requests]
;  (->> requests
;       (z/map dummy-response)
;       (time/delay 200)))
