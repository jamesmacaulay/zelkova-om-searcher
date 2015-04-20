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

(defn channel-input-handler
  [ch]
  (fn [e]
    (async/put! ch (.. e -target -value))))

(defn search-view
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      (let [text-input (z/write-port (:text data))]
        {:text-input-handler (channel-input-handler text-input)
         :signal             (z/map (fn [text] {:text text})
                                    text-input)}))
    om/IWillMount
    (will-mount [_]
      (->> (om/get-state owner :signal)
           (z/to-chan)
           (merge-channel-values-into-cursor! data)))
    om/IRenderState
    (render-state [_ state]
      (html
        [:div.row
         [:div.col-md-4]
         [:div.col-md-4
          [:label {:for "search-input"}
           "Search Wikipedia"]
          [:input {:class     "form-control"
                   :name      "search-input"
                   :type      "text"
                   :value     (:text data)
                   :on-change (:text-input-handler state)}]
          [:ul.list-unstyled
           (for [n (range 10)]
             [:li (str (:text data) " " n)])]]
         [:div.col-md-4]]))))

(defonce app-state (atom {:text "" :suggestions []}))

(om/root
  search-view
  app-state
  {:target (. js/document (getElementById "app"))})
