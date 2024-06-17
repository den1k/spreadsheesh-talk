(ns sst.ui.talk
  (:require
    [hyperfiddle.electric :as e]
    [hyperfiddle.electric-dom2 :as dom]
    [net.cgrand.xforms :as x]
    [stuffs.keybind :as keybind]
    [stuffs.util :as su]
    [tesserae.ui.electric-util :as eu]
    #?@(:clj [[nextjournal.markdown :as mkd]
              [clojure.java.io :as io]]))
  #?(:clj (:import (java.util Date))))

(e/defn Html [html]
  (e/client
    (dom/article
      (dom/props {:class [:flex :flex-col :prose :lg:prose-xl]})
      (dom/set-property! dom/node :innerHTML html))))

(e/defn Hiccup [h]
  (when-let [html (e/server (some-> h su/hiccup->html))]
    (new Html html)))

(e/defn Markdown [md]
  (when-let [html (e/server (some-> md mkd/->hiccup su/hiccup->html))]
    ;(println :ht html)
    (new Html html)
    #_(e/client
        (dom/article
          (dom/props {:class [:flex :flex-col :prose :lg:prose-xl]})
          (dom/set-property! dom/node :innerHTML html)))))

(e/defn RefWatcher
  ([a] (new RefWatcher nil a))
  ([label a]
   (e/server
     (let [v  (e/watch a)
           pv (some-> v su/pretty-string)]
       (when v
         (e/client
           (dom/div
             (dom/props {:style {:display :flex
                                 :gap     :5px}})
             (when label (dom/div (dom/text label)))
             (dom/pre (dom/props {:style {:margin 0}}) (dom/text pv)))))))))

(e/defn EditorWithEval [code]
  (e/client
    (let [[!code-str code-str] (eu/state code)
          [!result result] (eu/state "")
          eval (e/fn [e]
                 (reset! !result
                         (e/server
                           (some-> code-str read-string eval))))]
      (dom/div
        (dom/props {:class [:flex :flex-col :gap-1 :min-w-96]})
        (dom/div
          (dom/props {:class [:flex :gap-1 :border-2 :p-2 :w-full]})
          (dom/textarea
            (dom/props {:value code-str
                        :class [:h-16 :b2 :w-full]})
            (dom/on "keydown"
                    (e/fn [e]
                      (keybind/chord-case e
                        ("left" "right" "up" "down" "space") (.stopPropagation e)
                        "shift+enter" (do
                                        (.preventDefault e)
                                        (.stopPropagation e)
                                        (new eval nil))
                        )))
            (dom/on "input" (e/fn [e] (reset! !code-str (.. e -target -value)))))
          (dom/button
            (dom/props {:class ["border" "self-center" :p-2 :rounded]})
            (dom/on "click" eval)
            (dom/text "eval")))
        (when result
          (dom/code
            (dom/props {:class [:whitespace-pre :p-2 :overflow-scroll]})
            (dom/text result)))))))


(e/defn Slide [{:as x :keys [type content nested?]}]
  (e/client
    (dom/div
      (dom/props {:class ["flex" "justify-center" "h-full" :rounded
                          (when (not (contains? #{:v-box :h-box} type)) "border p-2")
                          (when-not nested? "overflow-scroll")]})
      (case type
        :markdown (new Markdown content)
        :hiccup (new Hiccup content)
        :html (new Html content)
        :ref-watcher (e/server (new RefWatcher content))
        :editor-with-eval (new EditorWithEval content)
        (:v-box :h-box) (dom/div
                          (dom/props {:class [:flex (when (= type :v-box) :flex-col) :gap-2]})
                          (e/server
                            (e/for [x content]
                              (new Slide (assoc x :nested? true)))))
        :electric (e/server (new content))))))

(def a (atom 5))
(comment
  (swap! a inc))

(defn file->slide
  [file-or-path]
  #?(:clj
     (when-let [s (slurp file-or-path)]
       {:type    :markdown
        :content s})))

(def md-slides
  #?(:cljs (constantly nil)
     :clj
     (let [sl (into []
                    (comp
                      ;(map str)
                      (x/sort-by str)
                      (map file->slide))
                    (.listFiles (io/file "resources/talk")))]
       (fn [slide-idx]
         (get sl (dec slide-idx))))))


(def slides
  [{:type    :ref-watcher
    :content a}
   (md-slides 1)
   (md-slides 2)
   (md-slides 3)
   {:type    :v-box
    :content [{:type    :html
               :content "<div class=\"font-serif\">this is <b>HTML</b></div>"}
              {:type    :hiccup
               :content [:div "this is " [:span {:class "font-mono"} "hiccup"]]}
              {:type    :markdown
               :content "this is **markdown**"}]}
   {:type :v-box
    :content
    [{:type    :hiccup
      :content [:div {:class ["font-xl" "font-mono"]} "Code editor demo"]}
     {:type    :h-box
      :content [
                {:type    :editor-with-eval
                 :content "(slurp \"deps.edn\")"}
                {:type    :editor-with-eval
                 :content "(java.util.Date.)"}]
      }]}

   {:type    :v-box
    :content [{:type    :editor-with-eval
               :content "(swap! sst.ui.talk/a inc)"}
              {:type    :ref-watcher
               :content a}]}

   {:type    :markdown
    :content "## Demo Time!"}
   {:type    :markdown
    :content "## End"}
   ])

(def dbg (atom nil))

(defonce !idx (atom 1))

(e/defn Slides []
  ; DBG
  #_(e/server (new RefWatcher "DBG:" dbg))
  (e/client
    (let [;[!idx idx]   (eu/state #_1 2)
          idx         (e/watch !idx)
          total-count (count slides)
          midx        (mod idx total-count)
          slide-type  (e/server (:type (nth slides midx)))]
      (e/server (reset! dbg idx))
      (dom/on js/document
              "keydown"
              (e/fn [e]
                (keybind/chord-case e
                  "left" (swap! !idx dec)
                  ("space" "right") (swap! !idx inc))))
      (dom/div
        (dom/props {:class ["flex" "p-3" "flex-col" "h-full"]})
        (e/server (new Slide (nth slides midx)))
        (dom/div
          (dom/props {:class ["flex" "gap-4" "w-full" "justify-between" "items-center"]})
          ; src viewer
          (dom/details
            (dom/props {:class ["outline-none"]})
            (dom/summary (dom/text "src"))
            (dom/code
              (dom/props {:class [:whitespace-pre :p-2]})
              (dom/text (su/pretty-string (e/server (nth slides midx))))))
          ; controls
          (dom/div (dom/props {:class ["flex" "gap-1" "self-end"]})
                   (dom/button (dom/on "click" (e/fn [_] (swap! !idx dec)))
                               (dom/text "<"))
                   (dom/text (str (inc midx) "/" total-count))
                   (dom/button (dom/on "click" (e/fn [_] (swap! !idx inc)))
                               (dom/text ">"))))))))


