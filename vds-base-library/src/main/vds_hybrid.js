(function() {
  if (window._vds_hybrid) {
    return;
  }
  _vds_hybrid_config = window._vds_hybrid_config || {};
  _vds_hybrid_config.imp = (_vds_hybrid_config.imp == undefined || _vds_hybrid_config.imp);
  var _vds_hybrid = {
    blacklistedClassRegex: /^(clear|clearfix|active|hover|enabled|hidden|display|focus|disabled|ng-|growing-)/,
    IMPRESSED_FLAG: "_impressed_",
    blacklistedTags: {
      "SCRIPT": 1,
      "STYLE": 1,
      "NOSCRIPT": 1,
      "IFRAME": 1,
      "BR": 1,
      "FONT": 1,
      "SVG": 1,
      "CANVAS": 1,
      "svg": 1,
      "canvas": 1,
      "tspan": 1,
      "text": 1,
      "g": 1,
      "rect": 1,
      "path": 1,
      "defs": 1,
      "clipPath": 1,
      "desc": 1,
      "title": 1
    },
    listTags: ["TR", "LI", "DL"],
    supportedClickTags: ["I", "SPAN"],
    supportedIconTags: ['A', 'BUTTON'],

    bind: function(fn, me) {
      return function() {
        return fn.apply(me, arguments);
      };
    },

    TaggingNode: function() {
      function TaggingNode(node) {
        var klass,
          ref;
        this.node = node;
        this.name = node.tagName.toLowerCase();
        klass = (ref = node.getAttribute("class")) != null ? ref.replace(
          /(^| )(clear|clearfix|active|hover|enabled|hidden|display|focus|disabled|ng-|growing-)[^\. ]*/g, "").trim() : void 0;
        if ((klass != null ? klass.length : void 0) > 0) {
          this.klass = klass.split(/\s+/).sort();
        }
        if (_vds_hybrid.utils.hasAttr(node, 'id') && (node.getAttribute('id').match(
            /^[0-9]/) === null)) {
          this.id = node.getAttribute('id');
        }
        if (_vds_hybrid.utils.hasAttr(node, 'href')) {
          this.href = node.getAttribute('href');
        }
        if (_vds_hybrid.utils.hasAttr(node, 'data-growing-info')) {
          this.grObj = node.getAttribute('data-growing-info');
        }
        if (_vds_hybrid.utils.hasAttr(node, 'data-growing-idx')) {
          this.grIdx = +node.getAttribute('data-growing-idx');
        }
      }

      TaggingNode.prototype.path = function() {
        var i,
          kls,
          len,
          level,
          ref;
        level = "/" + this.name;
        if (this.id != null) {
          level += "#" + this.id;
        }
        if (this.klass != null) {
          ref = this.klass;
          for (i = 0, len = ref.length; i < len; i++) {
            kls = ref[i];
            level += "." + kls;
          }
        }
        return level;
      };

      return TaggingNode;

    }(),
    path: function(node) {
      var cnode,
        depth,
        level,
        parentNode;
      depth = "";
      cnode = new _vds_hybrid.TaggingNode(node);
      while (cnode.name !== "body" && cnode.name !== "html") {
        level = cnode.path();
        depth = level + depth;
        parentNode = cnode.node.parentNode;
        if (parentNode && parentNode.tagName) {
          cnode = new _vds_hybrid.TaggingNode(parentNode);
        } else {
          break;
        }
      }
      return depth;
    },
    index: function(node) {
      var i,
        idx,
        len,
        n,
        pnode,
        ppnode,
        ref;
      pnode = node;
      while (pnode && pnode.tagName !== "BODY" && _vds_hybrid.utils.indexOf(_vds_hybrid.listTags,
          pnode.tagName) === -1) {
        pnode = pnode.parentNode;
      }
      if (pnode) {
        ppnode = pnode.parentNode;
        idx = 1;
        ref = ppnode.childNodes;
        for (i = 0, len = ref.length; i < len; i++) {
          n = ref[i];
          if (n.tagName !== pnode.tagName) {
            continue;
          }
          if (n === pnode) {
            return idx;
          }
          idx += 1;
        }
      }
    },
    isLeaf: function(node) {
      var cnode,
        i,
        len,
        ref;
      if (node.hasChildNodes()) {
        ref = node.childNodes;
        for (i = 0, len = ref.length; i < len; i++) {
          cnode = ref[i];
          if (cnode.nodeType === 1) {
            return false;
          }
        }
      }
      return true;
    },
    isParentOfLeaf: function(node) {
      var childNode,
        i,
        len,
        ref;
      if (!node.childNodes) {
        return false;
      }
      ref = node.childNodes;
      for (i = 0, len = ref.length; i < len; i++) {
        childNode = ref[i];
        if (!_vds_hybrid.isLeaf(childNode)) {
          return false;
        }
      }
      return true;
    },
    DomObserver: (function() {
      var mirrorClient,
        queuedMessages,
        times,
        version;

      function DomObserver() {
        this.pendingScanNodes = false;
        this.trackPageView = _vds_hybrid.bind(this.trackPageView, this);
      }

      DomObserver.prototype.registerDomObserver = function() {

        /*
         * Disconnect existed mirror client to aviod double observing
         */
        var domObserver = {
          impressNodes: (function(_this) {
            return function(children, force, snapshot, seqid) {
              var child,
                elems,
                i,
                len,
                message;

              _this.currentPath = _vds_hybrid.utils.path();
              _this.currentQuery = _vds_hybrid.utils.query();
              if (force && !snapshot || _vds_hybrid.resending) {
                _this.trackPageView(0, !_vds_hybrid.resending);
              }
              if (!snapshot && !_vds_hybrid_config.imp) {
                return;
              }
              message = {
                t: snapshot ? "snap" : "imp",
                d: window.location.host,
                tm: +Date.now(),
                ptm: _this.pageLoaded,
                p: _this.currentPath
              };
              if (_this.currentQuery.length > 0) {
                message.q = _this.currentQuery;
              }
              elems = [];
              for (i = 0, len = children.length; i < len; i++) {
                child = children[i];
                if (!child) {
                  continue;
                }
                elems = elems.concat(_this.getLeafNodes(child,
                  children.length, force, snapshot));
              }
              message.e = elems;
              if (seqid != undefined) {
                message.seqid = seqid;
              }
              if (elems.length > 0) {

                if (snapshot && _vds_hybrid.isMoving) {
                  var hitElems = [], cx = _vds_hybrid.circleHelper.curX, cy = _vds_hybrid.circleHelper.curY;
                  if (window._vds_bridge) {
                    cx *= devicePixelRatio * _vds_hybrid.circleHelper.initScale;
                    cy *= devicePixelRatio * _vds_hybrid.circleHelper.initScale;
                  } else {
                    cx *= _vds_hybrid.circleHelper.initScale;
                    cy *= _vds_hybrid.circleHelper.initScale;
                  }
                  for (var i = 0; i < elems.length; i++) {
                    var elem = elems[i];
                    if (cx >= elem.ex && cx <= elem.ex + elem.ew && cy >= elem.ey && cy <= elem.ey + elem.eh) {
                      hitElems.push(elem);
                    }
                  }
                  message.e = hitElems;
                }
                if (snapshot && window._vds_bridge) {
                  _vds_bridge.hoverNodes(JSON.stringify(message));
                } else {
                  _this.send(message);
                }
              } else if (window._vds_ios && snapshot) {
                _this.send({seqid: seqid})
              }
            };
          })(this)
        };
        _vds_hybrid.TreeMirror = new _vds_hybrid.TreeMirrorClient(document.body, domObserver);
        if (!_vds_hybrid.track) {
          _vds_hybrid.track = (function(_this) {
            return function (eventName, props) {
              if (!eventName || !props) {
                return;
              }
              var elem = {
                t: "cstm",
                d: window.location.host,
                p: _this.currentPath,
                ptm: _this.pageLoaded,
                tm: +Date.now(),
                n: eventName
              };
              if (_this.currentQuery) {
                elem.q = _this.currentQuery;
              }
              elem.e = props;
              if (window._vds_ios) {
                 _this.send(elem);
              } else if (window._vds_bridge) {
                _vds_bridge.saveCustomEvent(elem);
              }
            }
          })(this);
          if ("function" === typeof window.vdsHybridReadyCallback) {
            window.vdsHybridReadyCallback(_vds_hybrid.track);
          }
        }
        if (_vds_hybrid_config.imp == undefined || _vds_hybrid_config.imp) {
          if (window.MutationObserver) {
            var observer = new MutationObserver(function(mutations) {
              var added = [];
              try {
                  mutations.forEach(function(mutation) {
                    if (mutation.type === "attributes") {
                      added = added.concat(_vds_hybrid.TreeMirror.serializeAdded([mutation.target]));
                    } else {
                      added = added.concat(_vds_hybrid.TreeMirror.serializeAdded(mutation.addedNodes));
                    }
                  })
                  if (added.length > 0) _vds_hybrid.TreeMirror.mirror.impressNodes(added);
              } catch (e) {
                  console.log(e);
              }
            });
            observer.observe(document.body, {
              childList: true,
              subtree: true,
            });
          } else {
            _vds_hybrid.scanNodesAfterClick = true;
          }
        }
      };

      DomObserver.prototype.getLeafNodes = function(node, size, force, snapshot) {
        var childNode,
          i,
          leafs,
          len,
          message,
          parent_of_leaf,
          ref,
          ref1,
          ref2;
        leafs = [];
        parent_of_leaf = true;
        if (node.leaf) {
          if (node.nodeType === 1 && (((ref = node.attributes) != null ? ref.href :
              void 0) || (node.text != null))) {
            if (force || !node.known || snapshot || _vds_hybrid.resending) {
              message = this.nodeMessage(node, true, snapshot);
              if (size > 1) {
                message.idx = node.idx;
              }
              leafs.push(message);
            }
          }
        } else {
          ref1 = node.childNodes;
          childLeafs = []
          for (i = 0, len = ref1.length; i < len; i++) {
            childNode = ref1[i];
            if (!childNode.leaf) {
              parent_of_leaf = false;
            }
            childLeafs = childLeafs.concat(this.getLeafNodes(childNode, node.childNodes
              .length, force, snapshot));
          }
          if (_vds_hybrid.isMoving) {
           leafs = leafs.concat(childLeafs); 
          }
          if (parent_of_leaf && (snapshot || ((ref2 = node.attributes) != null ? ref2.any :
              void 0)) && (force || !node.known || snapshot)) {
            node.text = _vds_hybrid.utils.parentOfLeafText(node);
            if (node.childNodes && node.childNodes.length > 0 && node.childNodes[0].idx) {
              node.idx = node.childNodes[0].idx;
            }
            message = this.nodeMessage(node, false, snapshot);
            leafs.push(message);
          }
          if (!_vds_hybrid.isMoving) {
            leafs = leafs.concat(childLeafs);
          }
        }
        return leafs;
      };

      DomObserver.prototype.nodeMessage = function(node, leaf, snapshot) {
        var attributes,
          message,
          ref,
          ref1,
          ref2,
          ref3,
          plot,
          domNode;
        message = {
          x: node.path
        };
        if (snapshot) {
          message.ex = node.ex;
          message.ey = node.ey;
          message.ew = node.ew;
          message.eh = node.eh;
          message.nodeType = 'hybrid';
        }
        if (((ref = node.text) != null ? ref.length : void 0) > 0) {
          message.v = (ref1 = node.text) != null ? ref1.slice(0, 40) : void 0;
        } else if (!leaf && ((ref2 = node.text) != null ? ref2.length : void 0) === 0 && _vds_hybrid.utils.indexOf(_vds_hybrid.supportedIconTags, node.tagName) !== -1) {
          domNode = node.dom;
          if (domNode) {
            polt = (ref3 = domNode.innerText) != null ? ref3.trim() : void 0;
            if (polt && polt.length > 0 && polt.length < 50) {
              message.v = polt;
            }
          }
        }
        if (attributes = node.attributes) {
          if (attributes.href && attributes.href.indexOf('javascript') !== 0) {
            message.h = _vds_hybrid.utils.normalizePath(attributes.href.slice(0, 320));
            delete node.attributes.href;
          }
        }
        if (node.idx) {
          message.idx = node.idx;
        }
        if (node.obj) {
          message.obj = node.obj;
        }
        return message;
      };

      DomObserver.prototype.registerEventListener = function() {

        var addEventListener,
          blacklistedTags,
          eventHandler,
          events,
          supportedChangeTypes,
          supportedInputTypes,
          supportedTags,
          unsupportedSubTags;
        events = {
          click: "clck",
          change: "imp",
        // submit: "sbmt"
        };
        unsupportedSubTags = {
          "tspan": 1,
          "text": 1,
          "g": 1,
          "rect": 1,
          "path": 1,
          "defs": 1,
          "clipPath": 1,
          "desc": 1,
          "title": 1
        };
        blacklistedTags = ['TEXTAREA', 'HTML', 'BODY'];
        supportedInputTypes = ['button', 'submit'];
        supportedTags = ['A', 'BUTTON', 'INPUT', 'IMG'];
        supportedChangeTypes = ["radio", "checkbox"];

        addEventListener = function(element, eventType, eventHandler) {
          if (element.addEventListener) {
            return element.addEventListener(eventType, eventHandler, true);
          } else if (element.attachEvent) {
            return element.attachEvent('on' + eventType, eventHandler);
          } else {
            return element['on' + eventType] = eventHandler;
          }
        };
        eventHandler = (function(_this) {
          return function(event) {
            var elem,
              elemHref,
              i,
              imageParts,
              imageUrl,
              input,
              len,
              message,
              polt,
              ref,
              ref1,
              tagName,
              target,
              text;
            if (!document.body.className.match(/\bvds-entrytext\b/)) {
              target = event.target || event.srcElement;
              if (target.hasAttribute('growing-ignore')) {
                return;
              }
              while (target && unsupportedSubTags[target.tagName] === 1 &&
                target.parentNode) {
                target = target.parentNode;
              }
              if (_vds_hybrid.utils.indexOf(_vds_hybrid.supportedClickTags, target.tagName) !== -1 && target.parentNode && _vds_hybrid.utils.indexOf(_vds_hybrid.supportedIconTags, target.parentNode.tagName) !== -1) {
                target = target.parentNode;
              }
              tagName = target.tagName;
              if (event.type === 'click') {
                if (_vds_hybrid.utils.indexOf(blacklistedTags, tagName) !== -1) {
                  return;
                }
                if (tagName === 'INPUT' && _vds_hybrid.utils.indexOf(
                    supportedInputTypes, target.type) === -1) {
                  return;
                }
                if (_vds_hybrid.utils.indexOf(supportedTags, tagName) === -1 && !
                  _vds_hybrid.isLeaf(target) && !_vds_hybrid.isParentOfLeaf(target)) {
                  return;
                }
              }
              message = {
                d: window.location.host,
                ptm: _this.pageLoaded,
                tm: +Date.now(),
              };

              message.t = events[event.type];
              message.p = _this.currentPath;
              if (_this.currentQuery.length > 0) {
                message.q = _this.currentQuery;
              }
              elem = {};
              if (_vds_hybrid.circling) {
                elem.nodeType = 'hybrid';
              }
              elem.x = _vds_hybrid.path(target);
              if (elem.x.indexOf("/dl") !== -1 || elem.x.indexOf("/tr") !==
                -1 || elem.x.indexOf("/li") !== -1) {
                elem.idx = _vds_hybrid.index(target);
              }
              if (_vds_hybrid.utils.hasAttr(target, 'href')) {
                elemHref = target.getAttribute('href');
                if (elemHref) {
                  elem.h = _vds_hybrid.utils.normalizePath(elemHref.slice(0, 320));
                }
              }
              if (event.type === 'click' && target.getBoundingClientRect) {
                var frame = target.getBoundingClientRect();
                elem.ex = frame.left;
                elem.ey = frame.top;
                elem.ew = frame.width;
                elem.eh = frame.height;
              }
              if (event.type === 'click' && _vds_hybrid.isLeaf(target)) {
                if (tagName === "IMG") {
                  if (((ref = target.src) != null ? ref.indexOf(
                      "data:image") : void 0) === -1) {
                    elem.h = target.src;
                  }
                  if (target.alt) {
                    elem.v = target.alt;
                  } else if (elem.h) {
                    imageUrl = elem.h.split("?")[0];
                    imageParts = imageUrl.split("/");
                    if (imageParts.length > 0) {
                      elem.v = imageParts[imageParts.length - 1];
                    }
                  }
                } else if (tagName === "INPUT") {
                  elem.v = target.value;
                } else {
                  if (target.textContent != null) {
                    text = target.textContent.trim();
                    if (text.length > 0 && text.length < 50) {
                      elem.v = text;
                    }
                  } else if (target.innerText != null) {
                    text = target.innerText.trim();
                    if (text.length > 0 && text.length < 50) {
                      elem.v = text;
                    } else if (tagName === 'A') {
                      elem.v = text.slice(0, 30);
                    }
                  }
                }
              } else if (event.type === 'change' && "TEXTAREA" !==
                tagName && (("INPUT" === tagName && _vds_hybrid.utils.indexOf(
                  supportedChangeTypes, target.type) !== -1) ||
                "SELECT" === tagName)) {
                elem.v = target.value;
              } else if (event.type === 'submit') {
                ref1 = target.getElementsByTagName("input");
                for (i = 0, len = ref1.length; i < len; i++) {
                  input = ref1[i];
                  if (input.type === "search" || (input.type === "text" &&
                    (input.id === 'q' || input.id.indexOf("search") !==
                    -1 || input.className.indexOf("search") !== -1 ||
                    input.name === 'q' || input.name.indexOf(
                      "search") !== -1))) {
                    elem.x = _vds_hybrid.path(input);
                    elem.v = input.value.trim();
                  }
                }
              } else if (event.type === 'click' && _vds_hybrid.isParentOfLeaf(
                  target)) {
                polt = _vds_hybrid.utils.parentOfLeafText(target);
                if (polt.length > 0 && polt.length < 50) {
                  elem.v = polt;
                } else if (polt.length === 0 && _vds_hybrid.utils.indexOf(_vds_hybrid.supportedIconTags, tagName) !== -1) {
                  polt = target.innerText.trim();
                  if (polt.length > 0 && polt.length < 50) {
                    elem.v = polt;
                  }
                }
              }
              if (_vds_hybrid.utils.hasAttr(target, 'data-growing-title')) {
                elem.v = target.getAttribute('data-growing-title');
              }
              if (_vds_hybrid.utils.hasAttr(target, 'data-growing-idx')) {
                elem.idx = +target.getAttribute('data-growing-idx');
              }
              if (_vds_hybrid.utils.hasAttr(target, 'data-growing-info')) {
                elem.obj = target.getAttribute('data-growing-info');
              }
              message.e = [elem];
              if (event.type === 'click' && _vds_hybrid.scanNodesAfterClick) {
                if (_this.pengdingScanTimeout) {
                  clearTimeout(_this.pengdingScanTimeout);
                }
                _this.pendingScanNodes = true;
                _this.pengdingScanTimeout = setTimeout((function() {
                  _this.pendingScanNodes = false;
                  return _this.scanNewNodes();
                }), 500);
              }
              return _this.send(message);
            }
          };
        })(this);
        var results = [];
        for (var event in events) {
          results.push(addEventListener(document, event, eventHandler));
        }
        return results;
      };

      DomObserver.prototype.scanNewNodes = function() {
        _vds_hybrid.impressAllElements(false);
      };

      DomObserver.prototype.trackPageView = function(flag, refresh) {
        var key,
          msg;
        if (flag == null) {
          flag = 0;
        }
        if (this.pageLoaded == null || refresh) {
          this.pageLoaded = +Date.now();
        }
        msg = {
          v: document.title.slice(0, 255),
          t: "page",
          d: window.location.host,
          tm: this.pageLoaded,
          p: this.currentPath,
          rp: document.referrer
        };
        if (!this.currentQuery) {
          this.currentQuery = _vds_hybrid.utils.query();
        }
        if (this.currentQuery.length > 0) {
          msg.q = this.currentQuery;
        }
        if (flag > 0) {
          msg.fl = flag;
        }
        return this.send(msg);
      };

      DomObserver.prototype.registerHistoryHandler = function() {
        var pushState,
          replaceState;
        pushState = window.history.pushState;
        replaceState = window.history.replaceState;
        if (pushState != null) {
          window.history.pushState = (function(_this) {
            return function() {
              _this.prevUrl = window.location.toString();
              pushState.apply(window.history, arguments);
              return _this.pageChanged();
            };
          })(this);
        }
        if (replaceState != null) {
          window.history.replaceState = (function(_this) {
            return function() {
              _this.prevUrl = window.location.toString();
              replaceState.apply(window.history, arguments);
              return _this.pageChanged();
            };
          })(this);
        }
        if (pushState != null) {
          this.prevUrl = document.referrer;
          if (typeof Object.defineProperty === "function") {
            Object.defineProperty(document, "referrer", {
              get: (function(_this) {
                return function() {
                  return _this.prevUrl;
                };
              })(this),
              configurable: true
            });
          }
          _vds_hybrid.utils.bind(window, "popstate", _vds_hybrid.bind(this.pageChanged, this), true);
        }
        if (_vds_hybrid_config.hashtag) {
          return _vds_hybrid.utils.bind(window, "hashchange", _vds_hybrid.bind(this.pageChanged, this), true);
        }
      };

      DomObserver.prototype.pageChanged = function() {
        var newPath,
          newQuery,
          ref;
        newPath = _vds_hybrid.utils.path();
        newQuery = _vds_hybrid.utils.query();
        if (this.currentPath !== newPath || this.currentQuery !== newQuery) {
          if (_vds_hybrid_config.hashtag) {
            this.prevUrl = window.location.protocol + "//" + window.location.host +
            this.currentPath + this.currentQuery;
          }
          this.currentPath = newPath;
          this.currentQuery = newQuery;
          this.pageLoaded = +Date.now();
          this.trackPageView(1);
        }
      };

      DomObserver.prototype.domLoadedHandler = function(e) {
        if (this.domLoadedHandler.done) {
          return;
        }
        this.domLoadedHandler.done = true;
        this.registerEventListener();
        setTimeout(((function(_this) {
          return function() {
            return _this.registerDomObserver();
          };
        })(this)), 1000);
        if (window.history.pushState) {
          this.registerHistoryHandler();
        }
        return this;
      };

      DomObserver.prototype.blind = function() {
        var blind;
        blind = false;
        return blind;
      };

      DomObserver.prototype.observe = function(send) {
        var doScrollCheck,
          e,
          error,
          toplevel;
        this.send = send;

        /*
         * Only register once for each event listener to avoid duplicate listeners send same events multiple times
         */
        this.currentPath = _vds_hybrid.utils.path();
        this.currentQuery = _vds_hybrid.utils.query();
        this.trackPageView();
        if (document.addEventListener) {
          if (document.readyState === "interactive" || document.readyState ===
            "complete") {
            this.domLoadedHandler();
          } else {
            _vds_hybrid.utils.bind(document, "DOMContentLoaded", (function(_this) {
              return function() {
                return _this.domLoadedHandler();
              };
            })(this));
          }
        } else if (document.attachEvent) {
          _vds_hybrid.utils.bind(document, "onreadystatechange", (function(_this) {
            return function() {
              return _this.domLoadedHandler();
            };
          })(this));
          toplevel = false;
          try {
            toplevel = window.frameElement === null;
          } catch (error) {
            e = error;
          }
          if (document.documentElement.doScroll && toplevel) {
            doScrollCheck = (function(_this) {
              return function() {
                var error1;
                try {
                  document.documentElement.doScroll("left");
                } catch (error1) {
                  e = error1;
                  setTimeout(doScrollCheck, 1);
                  return;
                }
                return _this.domLoadedHandler();
              };
            })(this);
            doScrollCheck();
          }
        }
        _vds_hybrid.utils.bind(window, 'load', (function(_this) {
          return function() {
            return _this.domLoadedHandler();
          };
        })(this));
        _vds_hybrid.utils.bind(window, "beforeunload", (function(_this) {
          return function(e) {
            var te,
              tn;
            if (_this.pendingScanNodes) {
              _this.scanNewNodes();
              tn = +Date.now();
              te = tn + 300;
              while (tn < te) {
                tn = +Date.now();
              }
            }
          };
        })(this));
      };

      return DomObserver;

    })(),
  };

  window._vds_hybrid = _vds_hybrid;

  window._vds_hybrid.TreeMirrorClient = (function() {
    var supportedInputTypes = ['button', 'submit'];
    function TreeMirrorClient(target, mirror, testingQueries) {
      this.target = target;
      this.mirror = mirror;
      this.impressAllNode();
    }

    TreeMirrorClient.prototype.impressAllNode = function(force, snapshot, seqid) {
      var children = this.serializeTarget(snapshot);
      this.mirror.impressNodes(children, force, snapshot, seqid);
    };

    TreeMirrorClient.prototype.serializeTarget = function(snapshot) {
      var children = [];
      for (var child = this.target.firstChild; child; child = child.nextSibling) {
        var d = this.serializeNode(child, true, undefined, undefined, undefined, snapshot);
        if (d !== null) {
          children.push(d);
        }
      }
      return children;
    };

    TreeMirrorClient.prototype.serializeAdded = function(added, coordinate) {
      var _this = this;
      var all = added;

      if (all.length === 0) {
        return [];
      }

      var rootList = [];
      for (var i = 0; i < all.length; i++) {
        var node = all[i];
        if (node.nodeType == 3) {
          node = node.parentNode;
        }
        if (node.nodeType != 1 || node == document.body) {
          continue;
        }
        if (node && _vds_hybrid.blacklistedTags[node.tagName] !== 1) {
          var parent = node.parentNode;
          if (parent && parent.nodeType == 1) {
            var parentId = parent.getAttribute("id");
            var parentKlass = parent.getAttribute("class");
            var klass = node.getAttribute('class');
            if (parentId && (parentId.toLowerCase().indexOf("clock") !== -1 ||
              parentId.toLowerCase().indexOf("countdown") !== -1)) {
            } else if (parentKlass && (parentKlass.toLowerCase().indexOf("clock") !== -1 ||
              parentKlass.toLowerCase().indexOf("countdown") !== -1)) {
            } else if (parent.getAttribute('data-countdown')) {
            } else if (klass && klass.indexOf('daterangepicker') !== -1) {
            } else if (node.hasAttribute('growing-ignore')) {
            } else {
              while (parent && parent.tagName !== "BODY" && parent.nodeType == 1 && !parent.hasAttribute("growing-ignore")) {
                parent = parent.parentNode;
              }
              if (parent.tagName === "BODY") {
                rootList.push(node);
              }
            }
          }
        }
      }
      ;

      var moved = [];

      rootList.forEach(function(rootNode) {
        var parentIndex = undefined;
        var pnode = rootNode;
        while (pnode && pnode.tagName !== "BODY" && _vds_hybrid.listTags.indexOf(pnode.tagName) === -1) {
          pnode = pnode.parentNode;
        }
        if (pnode && pnode.tagName !== "BODY") {
          var ppnode = pnode.parentNode;
          var idx = 1;
          for (var n = ppnode.childNodes[idx - 1]; idx <= ppnode.childNodes.length; idx++) {
            if (n.tagName === pnode.tagName) {
              if (n === pnode) {
                parentIndex = idx;
                break;
              }
            }
          }
        }

        var data = _this.serializeNode(rootNode, true, undefined, parentIndex, undefined, coordinate);
        moved.push(data);
      });

      return moved;
    };

    TreeMirrorClient.prototype.serializeNode = function(node, recursive,
      depth, parentIndex, parentObj, coordinate) {
      if (node === null)
        return null;
      if (_vds_hybrid.blacklistedTags[node.tagName] === 1)
        return null;

      if (depth === undefined) {
        depth = "/";
        var parent = node.parentElement;
        while (parent && parent.tagName !== "BODY" && parent.tagName !==
          "HTML") {
          var level = "/" + parent.tagName.toLowerCase();
          var parentId = parent.getAttribute("id");
          if (parentId && (parentId.match(/^[0-9]/) === null)) {
            level += "#" + parentId;
          }
          if (parent.hasAttribute("class")) {
            var klasses = parent.getAttribute("class").trim().split(/\s+/).sort();
            for (var i = 0; i < klasses.length; i++) {
              if (klasses[i].length > 0 && _vds_hybrid.blacklistedClassRegex.exec(
                  klasses[i]) === null) {
                level += "." + klasses[i];
              }
            }
          }
          depth = level + depth;
          parent = parent.parentElement;
        }
      }

      var data = {
        nodeType: node.nodeType,
      };

      if (data.nodeType === 1 && _vds_hybrid.supportedIconTags.indexOf(node.tagName) !== -1) {
        data.dom = node;
      }

      switch (data.nodeType) {
        case 10: // Node.DOCUMENT_TYPE_NODE:
          var docType = node;
          data.name = docType.name;
          data.publicId = docType.publicId;
          data.systemId = docType.systemId;
          break;

        case 8: // Node.COMMENT_NODE:
          return null;

        case 3: // Node.TEXT_NODE:
          if (depth === "/" || node.textContent.trim().length === 0) {
            return null;
          }
          data.textContent = node.textContent.trim();
          if (data.textContent.length > 0) {
            data.leaf = true;
            data.text = data.textContent;
            data.path = depth.slice(0, -1);
          }
          break;

        case 1: // Node.ELEMENT_NODE:
          if (window.getComputedStyle(node).display === "none") {
            if (node.tagName !== 'A' && node.querySelector('a') === null) {
              return null;
            }
          }
          var elm = node;
          data.tagName = elm.tagName;
          data.attributes = {
            any: elm.hasAttributes()
          };
          if (coordinate && elm.getBoundingClientRect) {
            var rect = elm.getBoundingClientRect();
            if (_vds_hybrid.circleHelper.scaleFactor) {
              data.ex = rect.left / _vds_hybrid.circleHelper.scaleFactor;
              data.ey = rect.top / _vds_hybrid.circleHelper.scaleFactor;
              data.ew = rect.width / _vds_hybrid.circleHelper.scaleFactor;
              data.eh = rect.height / _vds_hybrid.circleHelper.scaleFactor;
            } else if (window._vds_bridge) {
              data.ex = rect.left * devicePixelRatio * _vds_hybrid.circleHelper.initScale;
              data.ey = rect.top * devicePixelRatio * _vds_hybrid.circleHelper.initScale;
              data.ew = rect.width * devicePixelRatio * _vds_hybrid.circleHelper.initScale;
              data.eh = rect.height * devicePixelRatio * _vds_hybrid.circleHelper.initScale;
            } else {
              data.ex = rect.left * _vds_hybrid.circleHelper.initScale;
              data.ey = rect.top * _vds_hybrid.circleHelper.initScale;
              data.ew = rect.width * _vds_hybrid.circleHelper.initScale;
              data.eh = rect.height * _vds_hybrid.circleHelper.initScale;
            }
          }
          data.known = elm[_vds_hybrid.IMPRESSED_FLAG];
          if (!data.known) elm[_vds_hybrid.IMPRESSED_FLAG] = 1;
          depth += elm.tagName.toLowerCase();
          if (elm.hasAttribute('id') && elm.getAttribute('id').match(
              /^[0-9]/) === null) {
            depth += "#" + elm.getAttribute('id');
          }
          if (elm.hasAttribute('class')) {
            klasses = elm.getAttribute('class').trim().split(/\s+/).sort();
            for (var i = 0; i < klasses.length; i++) {
              if (klasses[i].length > 0 && _vds_hybrid.blacklistedClassRegex.exec(
                  klasses[i]) === null) {
                depth += "." + klasses[i];
              }
            }
          }
          if (elm.hasAttribute('href')) {
            data.attributes.href = elm.getAttribute('href');
          }

          var isLeaf = true;
          var isLeafNode;
          depth += "/";
          if (recursive) {
            if (elm.childNodes.length > 0) {
              data.childNodes = [];

              if (elm.hasAttribute('growing-ignore')) {
                return null;
              } else {
                var idx = 0;
                var grObj, grIdx;
                for (var child = elm.firstChild; child; child = child.nextSibling) {
                  grObj = null;
                  grIdx = -1;
                  if (child.nodeType === 1) {
                    if (child.hasAttribute('growing-ignore')) {
                      continue;
                    }
                    if (_vds_hybrid.listTags.indexOf(child.tagName) !== -1) {
                      idx += 1;
                    }
                    if (child.hasAttribute('data-growing-idx')) {
                      idx = +child.getAttribute('data-growing-idx');
                      grIdx = idx;
                    }

                    if (child.hasAttribute('data-growing-info')) {
                      grObj = child.getAttribute('data-growing-info');
                    }
                  }
                  var d;
                  if (idx > 0) {
                    d = this.serializeNode(child, true, depth, idx, grObj || parentObj, coordinate);
                  } else {
                    d = this.serializeNode(child, true, depth, parentIndex, grObj || parentObj, coordinate);
                  }
                  if (d === null) {
                    isLeaf = false;
                  } else if (typeof (d.childNodes) !== "undefined") {
                    isLeaf = false;
                    isLeafNode = true;
                    for (var j = 0; j < d.childNodes.length; j++) {
                      if (d.childNodes[j].tagName) {
                        isLeafNode = false;
                        break;
                      }
                    }
                    if (isLeafNode) {
                      if (idx > 0 && grIdx > 0) {
                        d.idx = idx;
                      } else if (parentIndex) {
                        d.idx = parentIndex;
                      }
                      if (grObj) {
                        d.obj = grObj;
                      } else if (parentObj) {
                        d.obj = parentObj;
                      }
                    }
                    data.childNodes.push(d);
                  } else {
                    if (elm.offsetWidth === 0 || elm.offsetHeight === 0) {
                      if (elm.tagName !== 'A' && elm.tagName !== 'BUTTON') {
                        return null;
                      }
                    }
                    if (d.leaf) {
                      if (parentIndex) {
                        d.idx = parentIndex;
                      }
                      if (parentObj) {
                        d.obj = parentObj;
                      }
                      data.childNodes.push(d);
                    }
                  }
                }
              }
            } else {
              data.childNodes = [];
            }
          } else {
            if (elm.hasChildNodes()) {
              var parent_of_leaf = true;
              // see if any of the child nodes are elements
              for (var i = 0; i < elm.childNodes.length; i++) {
                if (elm.childNodes[i].nodeType === 1) { // Node.ELEMENT_NODE
                  // there is a child element, so return false to not include
                  // this parent element
                  isLeaf = false;
                  if (elm.childNodes[i].hasChildNodes()) {
                    for (var j = 0; j < elm.childNodes[i].childNodes.length; j++) {
                      if (elm.childNodes[i].childNodes[j].nodeType === 1) { // Node.ELEMENT_NODE
                        parent_of_leaf = false;
                        break;
                      }
                    }
                  }
                }
              }
              if (!isLeaf) {
                data.parent_of_leaf = parent_of_leaf;
                if (parent_of_leaf) {
                  var content = "",
                    childTextContent = "";
                  for (var i = 0; i < elm.childNodes.length; i++) {
                    if (elm.childNodes[i].nodeType === 3) { // Node.TEXT_NODE
                      childTextContent = elm.childNodes[i].textContent.trim();
                      if (childTextContent.length > 0) {
                        content += childTextContent + " ";
                      }
                    }
                  }
                  content = content.trim();
                  if (content.length > 0) {
                    data.text = content;
                  }
                }
              }
            }
            if (depth.indexOf("/dl") !== -1 || depth.indexOf("/tr") !== -1 ||
              depth.indexOf("/li") !== -1) {
              var pnode = elm;
              while (pnode && pnode.tagName !== "BODY" && _vds_hybrid.listTags.indexOf(
                  pnode.tagName) === -1) {
                pnode = pnode.parentNode;
              }
              if (pnode) {
                var ppnode = pnode.parentNode;
                var pidx = 1,
                  k,
                  len,
                  pnc;
                for (k = 0, len = ppnode.childNodes.length; k < len; k++) {
                  pnc = ppnode.childNodes[k];
                  if (pnc.tagName !== pnode.tagName) {
                    continue;
                  }
                  if (pnc === pnode) {
                    data.idx = pidx;
                  }
                  pidx += 1;
                }
              }
            }
          }
          if (isLeaf) {
            data.idx = parentIndex;
            data.leaf = true;
            if (elm.tagName === "IMG") {
              if (elm.src && elm.src.indexOf("data:image") === -1) {
                data.attributes.href = elm.src;
              }
              if (elm.alt) {
                data.text = elm.alt;
              } else if (data.attributes.href) {
                var imageUrl = data.attributes.href.split("?")[0];
                if (imageUrl) {
                  var imageParts = imageUrl.split("/");
                  if (imageParts.length > 0) {
                    data.text = imageParts[imageParts.length - 1];
                  }
                }
              }
            } else if (elm.tagName === "INPUT" && supportedInputTypes.indexOf(
                elm.type) !== -1) {
              data.text = elm.value;
            } else {
              var textContent = elm.textContent.trim();
              if (!coordinate && textContent.length === 0 && elm.tagName !== "I" && elm.tagName !==
                'A' && elm.tagName !== 'BUTTON') {
                return null;
              } else {
                data.text = textContent;
              }
            }
          }
          if (_vds_hybrid.utils.hasAttr(elm, 'data-growing-title')) {
            data.text = elm.getAttribute('data-growing-title');
          } else if (_vds_hybrid.utils.hasAttr(elm, 'title')) {
            data.text = elm.getAttribute('title');
          }
          data.path = depth.slice(0, -1);
          break;
      }

      return data;
    };

    return TreeMirrorClient;
  })()


  window._vds_hybrid.utils = {
    bind: function(elem, type, callback, useCapture) {
      if (useCapture == null) {
        useCapture = false;
      }
      if (document.addEventListener != null) {
        return elem.addEventListener(type, callback, useCapture);
      } else if (document.attachEvent != null) {
        return elem.attachEvent("on" + type, function() {
          var e;
          e = window.event;
          e.currentTarget = elem;
          e.target = e.srcElement;
          return callback.call(elem, e);
        });
      } else {
        return elem["on" + type] = callback;
      }
    },
    hasAttr: function(tag, attrName) {
      if (tag.hasAttribute) {
        return tag.hasAttribute(attrName);
      } else {
        return !!tag[attrName];
      }
    },
    path: function() {
      var hash, path;
      path = this.normalizePath(window.location.pathname);
      if (_vds_hybrid_config.hashtag) {
        hash = window.location.hash;
        if (hash.indexOf("?") !== -1) {
          return path += hash.split('?')[0];
        } else {
          return path += hash;
        }
      } else {
        return path;
      }
    },
    normalizePath: function(path) {
      var len;
      len = path.length;
      if (len > 1 && path[len - 1] === "/") {
        return path.slice(0, len - 1);
      } else {
        return path;
      }
    },
    query: function() {
      var query;
      query = window.location.search;
      if (query.length > 1 && query[0] === "?") {
        return query.slice(1);
      } else {
        return query;
      }
    },
    isEmpty: function(obj) {
      var prop;
      if ((function() {
          var i,
            len1,
            results;
          results = [];
          for (i = 0, len1 = obj.length; i < len1; i++) {
            prop = obj[i];
            results.push(obj.hasOwnProperty(prop));
          }
          return results;
        })()) {
        return false;
      }
      return true;
    },
    parentOfLeafText: function(node) {
      var childNode,
        childTextContent,
        content,
        i,
        len1,
        ref;
      content = "";
      if (!node.childNodes) {
        return "";
      }
      ref = node.childNodes;
      for (i = 0, len1 = ref.length; i < len1; i++) {
        childNode = ref[i];
        if (childNode.nodeType === 3) {
          if (childNode.textContent != null) {
            childTextContent = childNode.textContent.trim();
          } else if (childNode.data != null) {
            childTextContent = childNode.data.trim();
          }
          if (childTextContent.length > 0) {
            content += childTextContent + " ";
          }
        }
      }
      return content = content.trim();
    },
    indexOf: function(array, value) {
      var index,
        length,
        other;
      if (Array.prototype.indexOf != null) {
        return array.indexOf(value);
      } else {
        length = array.length;
        index = -1;
        while (++index < length) {
          other = array[index];
          if (other === value) {
            return index;
          }
        }
        return -1;
      }
    }
  };

  var hitElems = [];
  var hitElem;
  var showTagsAfterCircle = false;
  var invalidRectCache = true;
  var RECT_CACHE_ID = "_bounding_rect_";

  function VdsCircleHelper() {

    this.init = init;

    this.traverse = traverse;
    this.hitTest = hitTest;
    this.showMaskView = showMaskView;
    this.hideMaskView = hideMaskView;
    this.hoverOn = hoverOn;
    this.viewWidth = 0;

    this.lastViewNode = {};
  }

  function init() {
    this.hoverMaskView = document.createElement('div');
    this.hoverMaskView.id = 'vds-mask-view';
    this.hoverMaskView.style.position = 'fixed';
    this.hoverMaskView.style.backgroundColor = 'rgba(255, 72, 36, 0.3)';
    this.hoverMaskView.style.borderRadius = '3px';
    this.hoverMaskView.style.border = 'rgba(255, 72, 36,0.78) solid 1px';
    this.hoverMaskView.setAttribute('growing-ignore', "");
    this.hoverMaskView.style.margin = '0';
    this.hoverMaskView.style.padding = '0';
    this.hoverMaskView.style.width = '0';
    this.hoverMaskView.style.height = '0';
    this.hoverMaskView.style.left = '0';
    this.hoverMaskView.style.zIndex = '99999';
    this.hoverMaskView.style.display = 'none';
    this.initScale = 1;
    var meta = document.querySelector("meta[name='viewport']");
    if (meta && meta.content) {
      var kvs = meta.content.split(",");
      for (var i = 0; i < kvs.length; i++) {
        var s = kvs[i].split("=");
        console.log(s);
        if (s.length != 2) continue;
        var k = s[0];
        var v = s[1];
        if (k.trim() == "initial-scale") {
          this.initScale = parseFloat(v);
          if (this.initScale == NaN) {
            this.initScale = 1;
          }
          console.log("page scale="+this.initScale);
          break;
        }
      }
    }
  }

  function circlable(elem) {
    var height, ref, ref1, ref2, width;
    if (_vds_hybrid.blacklistedTags[elem.tagName] === 1 || elem.hasAttribute('growing-ignore')) {
      return false;
    }
    if (_vds_hybrid.isLeaf(elem) && ((ref = elem.innerText) != null ? ref.trim().length : void 0) < 50) {
      if (((ref1 = elem.innerText) != null ? ref1.trim().length : void 0) === 0) {
        rect = elem[RECT_CACHE_ID];
        width = rect.width;
        height = rect.height;
        if (width > window.innerWidth * 0.5 && height >= window.innerHeight * 0.5) {
          return false;
        }
      }
      return true;
    }
    if (_vds_hybrid.isParentOfLeaf(elem) && (((ref2 = elem.innerText) != null ? ref2.trim().length : void 0) > 0 || elem.hasAttributes())) {
      return true;
    }
    return false;
  };


  function traverse(viewNode, x, y) {
    var children = viewNode.childNodes;

    if (viewNode != document.body && hitTest(viewNode, x, y) && (viewNode.nodeName != 'INPUT' || (viewNode.type == 'submit' || viewNode.type == 'button'))
        && viewNode.id != 'vds-mask-view' && circlable(viewNode)) {
      hitElems.push(viewNode);
    }
    if (children) {
      for (var i = 0; i <= children.length - 1; i++) {
        if (!viewNode.nodeName || viewNode.nodeName == 'SCRIPT' || viewNode.nodeType === 3) continue;
        traverse(children[i], x, y);
      }
    }
  }

  function hoverOn(x, y) {
    x /= this.initScale;
    y /= this.initScale;
    if (x == this.curX && y == this.curY) return;
    this.curX = x;
    this.curY = y;
    _vds_hybrid.circling = true;
    _vds_hybrid.isMoving = true;
    if (!showTagsAfterCircle && _vds_hybrid.setShowCircledTags) {
      showTagsAfterCircle = _vds_hybrid.setShowCircledTags(false);
    }
    hitElems.splice(0, hitElems.length);
    traverse(document.body, x, y);
    invalidRectCache = false;
    if (hitElems.length > 0) {
      var elem = hitElems[hitElems.length - 1];
      if (elem && elem.tagName != 'BODY' && !elem.hasAttribute('growing-ignore')) {
        this.hoveredElem = elem;
        this.showMaskView(this.hoveredElem);
        return;
      }
    }
    this.hoveredElem = null;
    this.hideMaskView();
  }

  function hitTest(viewNode, x, y) {
    if (!viewNode.getBoundingClientRect) return;
    var rect = viewNode[RECT_CACHE_ID];
    if (!rect || invalidRectCache) {
      rect = viewNode.getBoundingClientRect();
      viewNode[RECT_CACHE_ID] = rect;
    }
    return rect.left <= x && rect.right > x && rect.top <= y && rect.bottom > y;
  }

  function showMaskView(viewNode) {
    if (!viewNode.getBoundingClientRect) return;
    if (!this.hoverMaskView.parentNode) {
      document.body.appendChild(this.hoverMaskView);
    }
    var rect = viewNode.getBoundingClientRect();
    this.hoverMaskView.style.left = (rect.left + "px");
    this.hoverMaskView.style.top = (rect.top + "px");
    this.hoverMaskView.style.width = (rect.width + "px");
    this.hoverMaskView.style.height = (rect.height + "px");
    this.hoverMaskView.style.display = 'block';
  }

  function hideMaskView() {
    this.hoverMaskView.style.display = 'none';
    invalidRectCache = true;
  }

  _vds_hybrid.cancelHover = function() {
    _vds_hybrid.circleHelper.hideMaskView();
    if (showTagsAfterCircle) {
      showTagsAfterCircle = false;
      _vds_hybrid.setShowCircledTags(true);
    }
  }

  _vds_hybrid.findElementAtPoint = function(seqid) {
    _vds_hybrid.circleHelper.hideMaskView();
    var hoverNodes = [];
    if (hitElems && hitElems.length > 0) {
      if (hitElems.length > 1) {
        var leafElems = [];
        for (var i = 0; i < hitElems.length; i++) {
          var parentOfLeaf = false;
          for (var j = hitElems.length - 1; j >= 0; j--) {
            if (i != j && hitElems[j] == hitElems[i].parentNode) {
              parentOfLeaf = true;
              break;
            }
          }
          if (!parentOfLeaf) {
            leafElems.push(hitElems[i]);
          }
        }
        hitElems = leafElems;
      }
      for (var i = hitElems.length - 1; i >= 0; i--) {
        var elem = hitElems[i];
        var xpath = _vds_hybrid.path(elem);
        if (xpath.indexOf("/dl") !== -1 || xpath.indexOf("/tr") !==
          -1 || xpath.indexOf("/li") !== -1) {
          var idx = _vds_hybrid.index(elem);
        }
        var node = _vds_hybrid.TreeMirror.serializeNode(elem, true, undefined, idx, undefined, true);
        if (node) hoverNodes.push(node);
      }
      _vds_hybrid.TreeMirror.mirror.impressNodes(hoverNodes, true, true, seqid);
    } else if (window._vds_ios) {
      _vds_hybrid.sendQueue({seqid: seqid});
    }
    _vds_hybrid.isMoving = false;
  }

  _vds_hybrid.snapshotAllElements = function(seqid) {
    _vds_hybrid.circling = true;
    _vds_hybrid.TreeMirror.impressAllNode(true, true, seqid);
  }

  _vds_hybrid.impressAllElements = function(force, seqid) {
    _vds_hybrid.resending = !force;
    _vds_hybrid.TreeMirror.impressAllNode(force, false, seqid);
    _vds_hybrid.resending = false;
  }

  _vds_hybrid.resendPage = function(refresh) {
    _vds_hybrid.tracker.trackPageView(0, refresh);
  }

  var vdsCircleHelper = new VdsCircleHelper();
  vdsCircleHelper.init();
  window._vds_hybrid.circleHelper = vdsCircleHelper;

  if (window._vds_bridge) {
    _vds_hybrid.hoverOn = function(x, y) {
      _vds_hybrid.circleHelper.hoverOn(x / devicePixelRatio, y / devicePixelRatio);
    };
  } else if (window._vds_ios) {
    _vds_hybrid.hoverOn = function(x, y, screenWidth) {
      _vds_hybrid.circleHelper.hoverOn(x, y, screenWidth);
    }

    _vds_hybrid.eventCount = 0;
    _vds_hybrid.readyToSend = true;
    _vds_hybrid.messageQueue = [];

    _vds_hybrid.sendQueue = function(data) {
      if (_vds_hybrid.UIWebView) {
        if (_vds_hybrid.readyToSend) {
          _vds_hybrid.sendWithIFrame([data]);
        } else {
          _vds_hybrid.messageQueue.push(data);
        }
      } else {
        window.webkit.messageHandlers.GrowingIO_WKWebView.postMessage([data]);
      }
    }

    _vds_hybrid.sendWithIFrame = function(data) {
      _vds_hybrid.readyToSend = false;
      var src = '/growinghybridsdk-' + _vds_hybrid.eventCount++ + "?" + encodeURIComponent(JSON.stringify(data));
      if (!_vds_hybrid.dataFrame) {
        _vds_hybrid.dataFrame = document.createElement('IFRAME');
        _vds_hybrid.dataFrame.style.width = 0;
        _vds_hybrid.dataFrame.style.height = 0;
        _vds_hybrid.dataFrame.style.margin = 0;
        _vds_hybrid.dataFrame.style.padding = 0;
        _vds_hybrid.dataFrame.style.border = 0;
        _vds_hybrid.dataFrame.style.position = "fixed";
        _vds_hybrid.dataFrame.style.display = "none";
        _vds_hybrid.dataFrame.src = src;
        document.body.appendChild(_vds_hybrid.dataFrame);
      } else {
        _vds_hybrid.dataFrame.src = src;
      }
    }

    _vds_hybrid.pollEvents = function() {
      if (_vds_hybrid.messageQueue.length > 0) {
        _vds_hybrid.sendWithIFrame(_vds_hybrid.messageQueue);
        _vds_hybrid.messageQueue = [];
      } else {
        _vds_hybrid.readyToSend = true;
      }
    }
  }

  _vds_hybrid.getPageInfo = function () {
    var info = {
      d: location.host,
      p: _vds_hybrid.utils.path(),
      q: _vds_hybrid.utils.query(),
      v: document.title.slice(0, 255)
    };
    if (window._vds_ios) {
        return JSON.stringify(info);
    } else {
        return info;
    }
  }

  _vds_hybrid.startTracing = function(viewType) {
    if (_vds_hybrid.tracker) {
      return;
    }
    _vds_hybrid.tracker = new _vds_hybrid.DomObserver();
    if (window._vds_bridge) {
      _vds_hybrid.tracker.observe(function(data) {
        _vds_bridge.saveEvent(JSON.stringify(data));
      });
    } else if (window._vds_ios) {
      _vds_hybrid.UIWebView = (viewType == "UIWebView");
      _vds_hybrid.tracker.observe(function(data) {
          if (data) _vds_hybrid.sendQueue(data);
        });
    }
  };

  if (window._vds_bridge) {
    _vds_hybrid.startTracing();
  }
  _vds_hybrid.version = '0.9.100';
  console.log('start');
})()