(ns ^:figwheel-always zelkova-om-searcher.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [zelkova-om-searcher.jsonp :as jsonp]
            [jamesmacaulay.zelkova.signal :as z]
            [jamesmacaulay.zelkova.time :as time]
            [cljs.core.async :as async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(defn search-request
  [text]
  {:url "http://en.wikipedia.org/w/api.php"
   :params {:action "opensearch"
            :search text
            :format "json"}})

(defn merge-channel-values-into-cursor!
  [cursor ch]
  (go-loop []
    (when-let [diff (<! ch)]
      (om/transact! cursor #(merge % diff))
      (recur))))

(defn search-view
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (html
        [:div.row
         [:div.col-md-4]
         [:div.col-md-4
          [:label {:for "search-input"}
           "Search Wikipedia"]
          [:input {:class "form-control"
                   :name  "search-input"
                   :type  "text"}]
          [:ul.list-unstyled
           (for [n (range 10)]
             [:li (str "suggestion " n)])]]
         [:div.col-md-4]]))))

(defonce app-state (atom {:text "" :suggestions []}))

(om/root
  search-view
  app-state
  {:target (. js/document (getElementById "app"))})
