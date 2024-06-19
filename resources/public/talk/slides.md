## Spreadsheesh!

[Tesserae](https://github.com/lumberdev/tesserae), a Clojure spreadsheet written in in [hyperfiddle/electric](https://github.com/hyperfiddle/electric)

![tesserae-screenshot](http://localhost:3200/talk/tesserae-screen.png)

---

## Before getting into the weeds with 🌿 Tesserae 🌿

### let’s look at a toy app 👀 🧸

---
## The slides app is the toy app

It's < 200 LoC of view code

### What can it do?

- slide navigation & rendering
- reading slides from the file system
- display slides in HTML, hiccup & markdown
- interactive code editor w/ server-side eval
- watch atoms and update reactively

---

## Tesserae Demo!

### EDN (Extensible Data Notation) support + pretty rendering

```
1
```
```
hello
```
```clojure
(range 10)
```

### Reactivity
```clojure
(render/as :ui/inc-dec 10)
;; call it num
($ "num")
($neighbor-ent :left)
(range 0 ($ "num"))

```

### I/O

```clojure
(http/get "https://lumber.dev")
```

### Rich Media Types

```clojure
(render/as
  :hiccup
  [:iframe
   {:src   "https://lumber.dev"
    :style {:width 600 :height 500}}])

```

### Automatic Execution

```clojure
(identity *cb-str*)
```
Enable: run on > *window focus*


```clojure
(render/as
  :hiccup
  (if (str/starts-with? *cb-str* "http")
    [:iframe
     {:src   *cb-str*
      :style {:width 600 :height 500}}]
    [:div "*cb-str* is not a url"]))
```
Enable: run on > *window focus*
copy: https://www.hyperfiddle.net/
copy: http://localhost:3200/talk

### Extensibility Example: AI

```clojure
;; AI function
(ai "write a haiku")
```

#### Extensibility: What about your code?

```clojure
(defn prompt-ai [user-prompt] <impl>)

(mount/with-args {:tesserae.eval/bindings {'ai prompt-ai}})
(mount/start)
```

Using your code:

```clojure
(if (str/starts-with? *cb-str* "http")
  (->> (http/get *cb-str*)
       :body
       (str "in 50 words or less, summarize the following HTML: ")
       ai)
  "will not summarize as *cb-str* is not a url")
```
Enable: run on > *window focus*

#### Scheduled Execution & Notifications

```clojure
(rand-int 100)
```
- add schedule: every 4 s
- turn on notifications
- notifs are multi-device and work even when tab is closed

#### Mobile Friendly

- panel view on mobile

---
## Live Examples (from Lumber Prod) 

### Scheduled Code Execution

#### Example: Slack bot for daily project budget updates

Scheduled on Tesserae:
![slack-notif-tesserae](http://localhost:3200/talk/slack-notif-tesserae.png)

Result on slack:
![slack-notif](http://localhost:3200/talk/slack-notif.png)


### Data Viz (Live)

- [hours / client](https://yield.theshopgrid.com/app/cell/12) 
- [hours by client / by person by week](https://yield.theshopgrid.com/app/cell/15) 
- [variable pricing / volume discount](https://yield.theshopgrid.com/app/sheet/85)

---

## Only ~ 4000 Lines of Code

*imagine how many LoC Google sheets is..*

```bash
➜  tesserae git:(master) ✗ find ./src -name '*' | xargs wc -l
wc: ./src: read: Is a directory
      21 ./src/logback.xml
wc: ./src/tesserae: read: Is a directory
wc: ./src/tesserae/ui: read: Is a directory
     125 ./src/tesserae/ui/typeahead.cljc
      34 ./src/tesserae/ui/vega.cljc
     100 ./src/tesserae/ui/popup.cljc
      30 ./src/tesserae/ui/panel.cljc
     187 ./src/tesserae/ui/task.cljc
     134 ./src/tesserae/ui/notif.cljc
      11 ./src/tesserae/ui/globals.cljc
     619 ./src/tesserae/ui/sheet.cljc
     307 ./src/tesserae/ui/views.cljc
      86 ./src/tesserae/ui/temp_views.cljc
      75 ./src/tesserae/ui/app.cljc
      28 ./src/tesserae/ui/render.cljc
     146 ./src/tesserae/ui/electric_util.cljc
     212 ./src/tesserae/db.clj
      78 ./src/tesserae/push_notif.clj
      64 ./src/tesserae/autoformat.cljc
     790 ./src/tesserae/eval.clj
      82 ./src/tesserae/serialize.cljc
wc: ./src/tesserae/eval: read: Is a directory
     292 ./src/tesserae/eval/schedule.cljc
       3 ./src/tesserae/eval/vars.cljc
wc: ./src/tesserae/ring: read: Is a directory
wc: ./src/tesserae/ring/middleware: read: Is a directory
     168 ./src/tesserae/ring/middleware/oauth2.clj
     419 ./src/tesserae/serve.clj
    4011 total

```

---

## Spreadsheet > Custom App

- no need to worry about layouts. X/Y coords provide an easily editable, low resolution grid UI builder
- reactivity enables composition without having to write custom glue code
- can be a dashboard, a code runner, an AI interface...
- great for powerful internal tools

---

# Talk done.
***
## Thanks!

### Open for Questions


- would not be possible without Electric! Big thanks to Dustin & Hyperfiddle team! 
- check out [Lumber.dev](https://lumber.dev)
- [Tesserae Repo](https://github.com/lumberdev/tesserae)
- [hyperfiddle/electric Repo](https://github.com/hyperfiddle/electric)

I'm [@denik](https://x.com/denik) on X and here's my email: [dennis@lumber.dev](mailto:dennis@lumber.dev)