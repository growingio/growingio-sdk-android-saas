(function() {
  if (window._vds_hybrid && window._vds_hybrid.setShowCircledTags) return;

  var GrXpathParser, TagStore, ToggleEye, UrlParseRE, Util, camelize, cssNumber, dasherize, isArray, maybeAddPx, rootNodeRE,
    bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    hasProp = {}.hasOwnProperty;

  GrXpathParser = (function() {
    function GrXpathParser(xpath) {
      this.toQuery = bind(this.toQuery, this);
      this.grXpath = xpath;
    }

    GrXpathParser.prototype.toQuery = function() {
      var rxpath;
      rxpath = this.grXpath.replace(/^\//, '').split('/').map((function(_this) {
        return function(t) {
          return _this._parseNode(t);
        };
      })(this));
      return rxpath.join(' > ');
    };

    GrXpathParser.prototype._parseNode = function(nodePath) {
      var char, filteredAttrs, j, len1, nodeAttrs, pi, prefixs;
      if (nodePath.indexOf('*') === 0) {
        return '*';
      }
      nodeAttrs = nodePath.split(/\.|\#/);
      prefixs = [''];
      if (nodeAttrs.length > 1) {
        for (j = 0, len1 = nodePath.length; j < len1; j++) {
          char = nodePath[j];
          if (['.', '#'].indexOf(char) !== -1) {
            prefixs.push(char);
          }
        }
        filteredAttrs = nodeAttrs.map(function(attr, i) {
          if (i === 0) {
            return attr.replace('*', '');
          }
          if (attr.match(/[^a-zA-Z0-9\-\_]/)) {
            return null;
          }
          return attr;
        });
        return filteredAttrs.map(function(attr, i) {
          if (attr) {
            return prefixs[i] + attr;
          }
        }).join('');
      } else {
        if (nodePath.indexOf('*') === 0) {
          return '*';
        } else {
          pi = nodePath.indexOf('*');
          if (pi === -1) {
            return nodePath;
          } else {
            return nodePath.substring(0, pi);
          }
        }
      }
    };

    return GrXpathParser;

  })();

  rootNodeRE = /^(?:body|html)$/i;

  UrlParseRE = /^(((([^:\/#\?]+:)?(?:(\/\/)((?:(([^:@\/#\?]+)(?:\:([^:@\/#\?]+))?)@)?(([^:\/#\?\]\[]+|\[[^\/\]@#?]+\])(?:\:([0-9]+))?))?)?)?((\/?(?:[^\/\?#]+\/+)*)([^\?#]*)))?(\?[^#]+)?)(#.*)?/;

  cssNumber = {
    'column-count': 1,
    'columns': 1,
    'font-weight': 1,
    'line-height': 1,
    'opacity': 1,
    'z-index': 1,
    'zoom': 1
  };

  camelize = function(str) {
    return str.replace(/-+(.)?/g, function(match, chr) {
      if (chr) {
        return chr.toUpperCase();
      } else {
        return '';
      }
    });
  };

  dasherize = function(str) {
    return str.replace(/::/g, '/').replace(/([A-Z]+)([A-Z][a-z])/g, '$1_$2').replace(/([a-z\d])([A-Z])/g, '$1_$2').replace(/_/g, '-').toLowerCase();
  };

  isArray = Array.isArray || (function(object) {
    return object instanceof Array;
  });

  if (Element.prototype.remove == null) {
    Element.prototype.remove = function() {
      if (this.parentNode) {
        return this.parentNode.removeChild(this);
      }
    };
  }

  maybeAddPx = function(name, value) {
    if (typeof value === "number" && !cssNumber[dasherize(name)]) {
      return value + "px";
    } else {
      return value;
    }
  };

  Util = (function() {
    function Util() {}

    Util.bind = function(elem, type, callback, useCapture) {
      var eProp;
      if (useCapture == null) {
        useCapture = false;
      }
      if (elem == null) {
        return;
      }
      if (document.addEventListener != null) {
        elem.addEventListener(type, callback, useCapture);
      } else if (document.attachEvent != null) {
        eProp = type + callback;
        elem['e' + eProp] = callback;
        elem[eProp] = function() {
          var e;
          e = window.event;
          e.currentTarget = elem;
          e.target = e.srcElement;
          return elem['e' + eProp].call(elem, e);
        };
        elem.attachEvent("on" + type, elem[eProp]);
      } else {
        elem["on" + type] = callback;
      }
      return true;
    };

    Util.unbind = function(elem, type, callback, useCapture) {
      var eProp;
      if (elem == null) {
        return;
      }
      if (document.removeEventListener != null) {
        elem.removeEventListener(type, callback, useCapture);
      } else if (document.detachEvent != null) {
        eProp = type + callback;
        elem.detachEvent("on" + type, elem[eProp]);
        elem[eProp] = null;
        elem['e' + eProp] = null;
      } else {
        elem["on" + type] = null;
      }
      return true;
    };

    Util.bindOn = function(container, event, selector, callback, useCapture) {
      var elem, j, len1, ref;
      if (useCapture == null) {
        useCapture = false;
      }
      switch (typeof selector) {
        case 'string':
          ref = container.querySelectorAll(selector);
          for (j = 0, len1 = ref.length; j < len1; j++) {
            elem = ref[j];
            Util.bindOnce(elem, event, callback, useCapture);
          }
          break;
        case 'function':
          if (callback !== null) {
            useCapture = callback;
          } else {
            useCapture = false;
          }
          callback = selector;
          Util.bindOnce(container, event, callback, useCapture);
      }
      return true;
    };

    Util.bindOnce = function(elem, event, callback, useCapture) {
      if (useCapture == null) {
        useCapture = false;
      }
      Util.unbind(elem, event, callback, useCapture);
      return Util.bind(elem, event, callback, useCapture);
    };

    Util.isLeaf = function(node) {
      var cnode, j, len1, ref;
      if (node.hasChildNodes()) {
        ref = node.childNodes;
        for (j = 0, len1 = ref.length; j < len1; j++) {
          cnode = ref[j];
          if (cnode.nodeType === Node.ELEMENT_NODE) {
            return false;
          }
        }
      }
      return true;
    };

    Util.isParentOfLeaf = function(node) {
      var cnode, j, len1, ref;
      if (!node.hasChildNodes()) {
        return false;
      }
      ref = node.childNodes;
      for (j = 0, len1 = ref.length; j < len1; j++) {
        cnode = ref[j];
        if (!Util.isLeaf(cnode)) {
          return false;
        }
      }
      return true;
    };

    Util.lessThanSomeLevelDepth = function(node, threshold, depth) {
      var childNodes, j, len1, n, ref;
      if (depth == null) {
        depth = 1;
      }
      childNodes = node.childNodes;
      if (childNodes.length > 0) {
        if (depth > threshold) {
          return false;
        }
        ref = node.childNodes;
        for (j = 0, len1 = ref.length; j < len1; j++) {
          n = ref[j];
          if (n.nodeType === Node.ELEMENT_NODE) {
            if (!Util.lessThanSomeLevelDepth(n, threshold, depth + 1)) {
              return false;
            }
          }
        }
      }
      return depth <= threshold;
    };

    Util.parentOfLeafText = function(node) {
      var childNode, childTextContent, content, j, len1, ref;
      content = "";
      if (!node.hasChildNodes()) {
        return "";
      }
      ref = node.childNodes;
      for (j = 0, len1 = ref.length; j < len1; j++) {
        childNode = ref[j];
        if (childNode.nodeType === 3) {
          if (childNode.textContent != null) {
            childTextContent = this.trim(childNode.textContent);
          } else if (childNode.data != null) {
            childTextContent = this.trim(childNode.data);
          }
          if (childTextContent.length > 0) {
            content += childTextContent + " ";
          }
        }
      }
      content = this.trim(content);
      if (content.length === 0 && ['A', 'BUTTON'].indexOf(node.tagName) !== -1) {
        return this.trim(node.innerText);
      } else {
        return content;
      }
    };

    Util.tree = function(node) {
      var cnode, tree;
      tree = [];
      cnode = new TaggingNode(node);
      while (cnode.name !== 'body' && cnode.name !== 'html') {
        tree.unshift(cnode);
        cnode = new TaggingNode(cnode.node.parentNode);
      }
      return tree;
    };

    Util.path = function(node) {
      var depth, j, len1, tn, tree;
      depth = "";
      tree = this.tree(node);
      for (j = 0, len1 = tree.length; j < len1; j++) {
        tn = tree[j];
        depth += tn.path();
      }
      return depth;
    };

    Util.index = function(node) {
      var idx, j, len1, n, pnode, ppnode, ref;
      pnode = node;
      while (pnode && pnode.tagName !== "BODY" && ["TR", "LI", "DL"].indexOf(pnode.tagName) === -1) {
        pnode = pnode.parentNode;
      }
      if (pnode) {
        ppnode = pnode.parentNode;
        idx = 1;
        ref = ppnode.childNodes;
        for (j = 0, len1 = ref.length; j < len1; j++) {
          n = ref[j];
          if (n.tagName !== pnode.tagName) {
            continue;
          }
          if (n === pnode) {
            return idx;
          }
          idx += 1;
        }
      }
    };

    Util.similarPath = function(node) {
      var cnode, idSelected, selector;
      selector = "";
      idSelected = 0;
      cnode = new TaggingNode(node);
      while (cnode.name !== 'body') {
        if (cnode.id != null) {
          if (idSelected) {
            selector = "" + (cnode.similarPath(true)) + selector;
            return this.path(cnode.node.parentNode) + selector;
          } else {
            idSelected = true;
          }
        }
        selector = "" + (cnode.similarPath()) + selector;
        cnode = new TaggingNode(cnode.node.parentNode);
      }
      return selector;
    };

    Util.hasClass = function(el, selector) {
      if (el == null) {
        return false;
      }
      return el.classList.contains(selector);
    };

    Util.addClass = function(el, klass) {
      if (el == null) {
        return false;
      }
      return el.classList.add(klass);
    };

    Util.removeClass = function(el, klass) {
      if (el == null) {
        return false;
      }
      return el.classList.remove(klass);
    };

    Util.toggleClass = function(el, klass) {
      if (this.hasClass(el, klass)) {
        return this.removeClass(el, klass);
      } else {
        return this.addClass(el, klass);
      }
    };

    Util.offset = function(elem) {
      var doc, docElem, offset, rect, win;
      if (!elem) {
        return;
      }
      rect = elem.getBoundingClientRect();
      if (rect.width || rect.height || elem.getClientRects().length) {
        doc = elem.ownerDocument;
        win = elem === elem.window ? elem : (elem.nodeType === 9 ? elem.defaultView : window);
        docElem = doc.documentElement;
        offset = {
          top: rect.top + win.pageYOffset - docElem.clientTop,
          left: rect.left + win.pageXOffset - docElem.clientLeft
        };
        return offset;
      }
    };

    Util.position = function(elem) {
      var obj, offset, offsetParent, parentOffset;
      offsetParent = Util.offsetParent(elem);
      offset = Util.offset(elem);
      if (!offset) {
        return null;
      }
      parentOffset = rootNodeRE.test(offsetParent.nodeName) ? {
        top: 0,
        left: 0
      } : Util.offset(offsetParent);
      parentOffset.top += parseFloat(Util.css(offsetParent, 'border-top-width') || 0);
      parentOffset.left += parseFloat(Util.css(offsetParent, 'border-left-width') || 0);
      return obj = {
        top: offset.top - parentOffset.top,
        left: offset.left - parentOffset.left
      };
    };

    Util.offsetParent = function(elem) {
      var parent;
      parent = elem.offsetParent || document.body;
      while (parent && !rootNodeRE.test(parent.nodeName) && Util.css(parent, "position") === "static") {
        parent = parent.offsetParent;
      }
      return parent;
    };

    Util.width = function(elem) {
      var rect;
      if (!elem) {
        return;
      }
      if (elem === elem.window) {
        return elem.innerWidth;
      } else if (elem.nodeType === 9) {
        return elem.documentElement.scrollWidth;
      } else {
        rect = elem.getBoundingClientRect();
        return Math.round(rect.width);
      }
    };

    Util.height = function(elem) {
      var rect;
      if (!elem) {
        return;
      }
      if (elem === elem.window) {
        return elem.innerHeight;
      } else if (elem.nodeType === 9) {
        return elem.documentElement.scrollHeight;
      } else {
        rect = elem.getBoundingClientRect();
        return Math.round(rect.height);
      }
    };

    Util.isHidden = function(node) {
      return node.style.display === "none";
    };

    Util.closest = function(el, fn) {
      while (el) {
        if (fn(el)) {
          return el;
        }
        el = el.parentNode;
      }
      return null;
    };

    Util.getElementsByXpath = function(xpath, content, index, href) {
      var arrayDoms, doms, error, error1, grXpathParser, rxpath;
      if (xpath) {
        try {
          grXpathParser = new GrXpathParser(xpath);
          rxpath = grXpathParser.toQuery();
          doms = document.querySelectorAll(rxpath);
        } catch (error1) {
          error = error1;
          console.warn(error);
          doms = [];
        }
        arrayDoms = Array.prototype.slice.call(doms);
        if (content || href || index) {
          arrayDoms = arrayDoms.filter(function(dom) {
            var domContent, matched, tagName;
            if (index) {
              var elemIndex = Util.index(dom);
              matched = elemIndex == index;
            } else {
              matched = true;
            }
            if (content) {
              domContent = Util.content(dom);
              matched = matched && domContent.length > 0 && domContent.indexOf(content) !== -1;
            }
            if (href) {
              tagName = dom.tagName.toLowerCase();
              if (tagName === "img") {
                matched = matched && dom.src && dom.src.indexOf("data:image") === -1 && dom.src === href;
              } else {
                matched = matched && dom.hasAttribute('href') && Util.normalizePath(dom.getAttribute('href')) === href;
              }
            }
            return matched;
          });
        }
        return arrayDoms;
      } else {
        return [];
      }
    };

    Util._xpathRemoveEmptyClassOrId = function(parsedXpath) {
      var res;
      res = parsedXpath.replace(/(\S)\#\./g, '$1\.').replace(/(\S)\#\s/g, '$1 ');
      res.replace(/\.(\s)/g, '$1').replace(/(\S)\.\s/, '$1 ');
      return res;
    };

    Util.content = function(dom) {
      var tagName;
      tagName = dom.tagName.toLowerCase();
      if (tagName === "img") {
        return dom.alt;
      } else if (tagName === "input") {
        return this.trim(dom.value);
      } else if (Util.isLeaf(dom)) {
        return this.trim(dom.textContent);
      } else if (Util.isParentOfLeaf(dom)) {
        return Util.parentOfLeafText(dom);
      } else {
        return "";
      }
    };

    Util.isEmpty = function(obj) {
      var prop;
      if ((function() {
        var j, len1, results;
        results = [];
        for (j = 0, len1 = obj.length; j < len1; j++) {
          prop = obj[j];
          results.push(obj.hasOwnProperty(prop));
        }
        return results;
      })()) {
        return false;
      }
      return true;
    };

    Util.aElementsEqualbElements = function(objA, objB) {
      var key, value;
      if (objA === objB) {
        return true;
      }
      if ((objA == null) || (objB == null)) {
        return false;
      }
      for (key in objA) {
        if (!hasProp.call(objA, key)) continue;
        value = objA[key];
        if (!objB.hasOwnProperty(key)) {
          return false;
        }
        if (value !== objB[key]) {
          return false;
        }
      }
      for (key in objB) {
        if (!hasProp.call(objB, key)) continue;
        value = objB[key];
        if (!objA.hasOwnProperty(key)) {
          return false;
        }
      }
      return true;
    };

    Util.offsetTop = function(obj) {
      var curtop;
      if (!obj.offsetParent) {
        return 0;
      }
      curtop = obj.offsetTop;
      while (obj = obj.offsetParent) {
        curtop += obj.offsetTop;
      }
      return curtop;
    };

    Util.title = function() {
      var parts, title;
      title = document.title;
      if (title.indexOf('|') !== -1) {
        parts = title.split('|');
        if (parts.length >= 3) {
          return this.trim(parts[0] + "|" + parts[1]);
        } else {
          return parts[0].trim();
        }
      } else if (title.indexOf('-') !== -1) {
        return this.trim(title.split('-')[0]);
      } else {
        return title;
      }
    };

    Util.host = function() {
      var j, len1, matches, r, rule, url;
      if (window.rules != null) {
        url = window.location.toString();
        for (j = 0, len1 = rules.length; j < len1; j++) {
          rule = rules[j];
          r = rule.split(",");
          url = url.replace(new RegExp(r[0]), r[1]);
        }
        matches = UrlParseRE.exec(url);
        return matches[10];
      } else {
        return window.location.host;
      }
    };

    Util.path = function() {
      var hash, j, len1, matches, path, r, rule, url;
      if (window.rules != null) {
        url = window.location.toString();
        for (j = 0, len1 = rules.length; j < len1; j++) {
          rule = rules[j];
          r = rule.split(",");
          url = url.replace(new RegExp(r[0]), r[1]);
        }
        matches = UrlParseRE.exec(url);
        path = matches[13];
        if (path == null) {
          path = "";
        }
      } else {
        path = window.location.pathname;
      }
      path = this.normalizePath(path);
      if (window.vds.hashtag) {
        hash = window.location.hash;
        if (hash.indexOf("?") !== -1) {
          return path += hash.split('?')[0];
        } else {
          return path += hash;
        }
      } else {
        return path;
      }
    };

    Util.query = function() {
      var j, len1, matches, query, r, rule, url;
      if (window.rules != null) {
        url = window.location.toString();
        for (j = 0, len1 = rules.length; j < len1; j++) {
          rule = rules[j];
          r = rule.split(",");
          url = url.replace(new RegExp(r[0]), r[1]);
        }
        matches = UrlParseRE.exec(url);
        query = matches[16];
        if (query == null) {
          query = "";
        }
      } else {
        query = window.location.search;
      }
      query = this.normalizeQuery(query);
      return query;
    };

    Util.normalizePath = function(path) {
      var fullPath, len;
      fullPath = this.trim(path);
      len = fullPath.length;
      if (len > 1 && fullPath[len - 1] === "/") {
        return fullPath.slice(0, len - 1);
      } else {
        return fullPath;
      }
    };

    Util.normalizeQuery = function(query) {
      var fullQuery, len;
      fullQuery = query.trim();
      len = fullQuery.length;
      if (len > 1 && fullQuery[0] === '?') {
        return fullQuery.slice(1);
      } else {
        return fullQuery;
      }
    };

    Util.css = function(element, property, value) {
      var computedStyle, css, j, key, len1, prop, props, results;
      if (!element) {
        return;
      }
      if (arguments.length < 3) {
        computedStyle = getComputedStyle(element, '');
        if (typeof property === 'string') {
          return element.style[camelize(property)] || computedStyle.getPropertyValue(property);
        } else if (isArray(property)) {
          props = {};
          for (j = 0, len1 = property.length; j < len1; j++) {
            prop = property[j];
            props[prop] = element.style[camelize(prop)] || computedStyle.getPropertyValue(prop);
          }
          return props;
        }
      }
      css = '';
      if (typeof property === 'string') {
        if (!value && value !== 0) {
          return element.style[dasherize(property)] = "";
        } else {
          return element.style[dasherize(property)] = maybeAddPx(property, value);
        }
      } else {
        results = [];
        for (key in property) {
          if (!property[key] && property[key] !== 0) {
            results.push(element.style[dasherize(key)] = "");
          } else {
            results.push(element.style[dasherize(key)] = maybeAddPx(key, property[key]));
          }
        }
        return results;
      }
    };

    Util.hasBackgroundImage = function(elem) {
      var imageStyle;
      imageStyle = Util.css(elem, 'background-image');
      return imageStyle && imageStyle.length > 0 && imageStyle !== "none";
    };

    Util.cursorOffset = function(event) {
      var docElement, offset;
      offset = {
        top: 0,
        left: 0
      };
      if (event.pageX) {
        offset.left = event.pageX;
        offset.top = event.pageY;
      } else if (event.clientX) {
        docElement = document.documentElement || document.body;
        offset.left = event.clientX + docElement.scrollLeft;
        offset.top = event.clientY + docElement.scrollTop;
      }
      return offset;
    };

    Util.merge = function(obj1, obj2) {
      var attrname, obj3;
      obj3 = {};
      for (attrname in obj1) {
        obj3[attrname] = obj1[attrname];
      }
      for (attrname in obj2) {
        obj3[attrname] = obj2[attrname];
      }
      for (attrname in obj3) {
        if (typeof obj3[attrname] === 'function') {
          delete obj3[attrname];
        }
      }
      return obj3;
    };

    Util.trim = function(str) {
      return str.replace(/^\s+/, "").replace(/\s+$/, "");
    };

    Util.levelDomain = function(domain) {
      var array;
      array = domain.split(".");
      if (array.length === 2) {
        return "." + array.join(".");
      } else if (array.length >= 3 && array[array.length - 2] === "com") {
        return "." + array.slice(-3).join(".");
      } else {
        return "." + array.slice(-2).join(".");
      }
    };

    return Util;

  })();

  TagStore = (function() {
    function TagStore() {
      this.tags = {};
      this.screenshots = {};
    }

    TagStore.prototype.add = function(tag) {
      return this.tags[tag.id] = tag;
    };

    TagStore.prototype.get = function(tagId) {
      return this.tags[tagId];
    };

    TagStore.prototype.getFromDom = function(dom) {
      return this.get(dom.getAttribute('data-tag-id')) || this.getByAttrs(dom);
    };

    TagStore.prototype.pages = function() {
      var id, pts;
      pts = [];
      for (id in this.tags) {
        if (this.tags[id].eventType === 'page') {
          pts.push(this.tags[id]);
        }
      }
      return pts;
    };

    TagStore.prototype.addScreenshot = function(screenshot) {
      var hashcode;
      hashcode = randomString(8);
      this.screenshots[hashcode] = screenshot;
      return hashcode;
    };

    TagStore.prototype.getScreenshot = function(hashcode) {
      return this.screenshots[hashcode];
    };

    TagStore.prototype.getByAttrs = function(dom) {
      var content, domain, href, path, query, t, taggingObject, tagsArray, xpath;
      taggingObject = new TaggingObject();
      taggingObject.make(dom);
      domain = Util.host();
      path = Util.path();
      if (Util.query().length > 0) {
        query = Util.query();
      }
      content = taggingObject.value;
      xpath = taggingObject.path();
      if (taggingObject.href != null) {
        href = Util.normalizePath(taggingObject.href);
      }
      tagsArray = [];
      for (t in this.tags) {
        if (typeof t === 'object') {
          tagsArray.push(this.tags[t]);
        }
      }
      return tagsArray.filter(function(t) {
        return t.attrs.domain === domain && t.attrs.path === path && t.attrs.query === query && t.attrs.content === content && t.attrs.xpath === xpath && t.attrs.href === href;
      })[0];
    };

    return TagStore;

  })();

  ToggleEye = (function() {
    function ToggleEye(tagStore, circleStyle) {
      this.tagStore = tagStore;
      this.circleStyle = circleStyle;
      this.toggled = true;
    }

    ToggleEye.prototype.toggle = function(target) {
      this.toggled = !this.toggled;
      if (this.toggled) {
        return this.on();
      } else {
        return this.off();
      }
    };

    ToggleEye.prototype.off = function() {
      this.toggled = false;
      this.circleStyle.cleanTagged();
    };

    ToggleEye.prototype.on = function() {
      this.toggled = true;
      var id, ref, results, tag;
      ref = this.tagStore.tags;
      results = [];
      for (id in ref) {
        tag = ref[id];
        results.push(this.tagOn(tag));
      }
      return results;
    };

    ToggleEye.prototype.tagOn = function(tag) {
      if (tag.eventType !== "page") {
        return this.circle(tag);
      }
    };

    ToggleEye.prototype.tagOff = function(tag) {
      if (tag.eventType !== "page") {
        return this.uncircle(tag);
      }
    };

    ToggleEye.prototype.circle = function(tag) {
      var domNodes;
      domNodes = Util.getElementsByXpath(tag.filter.xpath, (tag.filter.content ? tag.filter.content : null), tag.filter.index, (tag.filter.href ? tag.filter.href : null));
      domNodes.forEach((function(_this) {
        return function(dom) {
          if (typeof dom === "undefined") {
            return;
          }
          _this.circleStyle.taggedOn(dom);
          return dom.setAttribute('data-tag-id', tag.id);
        };
      })(this), tag);
      return domNodes;
    };

    ToggleEye.prototype.uncircle = function(tag) {
      var domNodes;
      domNodes = Util.getElementsByXpath(tag.attrs.xpath, (tag.filter.content ? tag.attrs.content : null), tag.attrs.index);
      domNodes.forEach((function(_this) {
        return function(dom) {
          _this.circleStyle.taggedOff(dom);
        };
      })(this), tag);
      return domNodes;
    };

    return ToggleEye;

  })();

  CircleStyle = (function() {
    function CircleStyle() {
      this.clickClass = "growing-circle-clicked";
      this.hoverClass = "growing-circle-hovered";
      this.similarClass = "growing-circle-similar";
      this.hiddenClass = "growing-circle-hidden";
      this.taggedClass = "growing-circle-tagged";
      this.taggedSimilarClass = "growing-circle-tagged-similar";
      this.coverClass = "growing-circle-cover";
      this.taggedCoverClass = "growing-circle-tagged-cover";
      this.hoverCoverClass = "growing-circle-hover-cover";
      this.simpleMode = true;
    }

    CircleStyle.prototype.toggleMode = function() {
      return this.simpleMode = !this.simpleMode;
    };

    CircleStyle.prototype.circleable = function(elem) {
      var height, ref, ref1, ref2, width;
      if (Util.hasClass(elem, this.taggedCoverClass) || Util.hasClass(elem, this.hoverCoverClass) || Util.hasClass(elem, this.coverClass)) {
        return false;
      }
      if (!this.simpleMode) {
        return true;
      }
      if (['A', 'BUTTON', 'INPUT', 'IMG'].indexOf(elem.tagName) !== -1) {
        return true;
      }
      if (Util.isLeaf(elem) && ((ref = elem.innerText) != null ? ref.trim().length : void 0) < 50) {
        if (((ref1 = elem.innerText) != null ? ref1.trim().length : void 0) === 0) {
          width = Util.width(elem);
          height = Util.height(elem);
          if (width > window.innerWidth * 0.5 && height >= window.innerHeight * 0.5) {
            return false;
          }
        }
        return true;
      }
      if (Util.isParentOfLeaf(elem) && (((ref2 = elem.innerText) != null ? ref2.trim().length : void 0) > 0 || elem.hasAttributes())) {
        return true;
      }
      if (Util.hasBackgroundImage(elem) && Util.lessThanSomeLevelDepth(elem, 4)) {
        return true;
      }
      return false;
    };

    CircleStyle.prototype.getClick = function() {
      return document.getElementsByClassName(this.clickClass)[0];
    };

    CircleStyle.prototype.isCover = function(elem) {
      return Util.hasClass(elem, this.coverClass);
    };

    CircleStyle.prototype.isClick = function(elem) {
      return Util.hasClass(elem, this.clickClass);
    };

    CircleStyle.prototype.findTarget = function(elem) {
      if (Util.hasClass(elem, this.hoverCoverClass) || Util.hasClass(elem, this.coverClass)) {
        return elem = elem.parentNode.querySelector("*[data-target=" + (elem.getAttribute("data-orig")) + "]");
      } else if (Util.hasClass(elem, this.taggedCoverClass)) {
        return elem = elem.parentNode.querySelector("*[data-tagged-target=" + (elem.getAttribute("data-orig")) + "]");
      } else {
        return elem;
      }
    };

    CircleStyle.prototype.clickOn = function(elem) {
      var hashcode;
      elem = this.findTarget(elem);
      if (this.circleable(elem)) {
        hashcode = randomString(8);
        this.appendCover(elem, this.coverClass, "click-" + hashcode);
        Util.addClass(elem, this.clickClass);
        elem.setAttribute("data-target", "click-" + hashcode);
        return elem;
      } else {
        return false;
      }
    };

    CircleStyle.prototype.clickOff = function(elem) {
      var celem, i, len, ref, results;
      Util.removeClass(elem, this.clickClass);
      ref = document.querySelectorAll('.' + this.coverClass);
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        celem = ref[i];
        results.push(celem.remove());
      }
      return results;
    };

    CircleStyle.prototype.isHover = function(elem) {
      return Util.hasClass(elem, this.hoverClass);
    };

    CircleStyle.prototype.hoverIn = function(elem) {
      if (Util.hasClass(elem, this.hoverCoverClass)) {
        return;
      }
      if (this.circleable(elem)) {
        return Util.addClass(elem, this.hoverClass);
      }
    };

    CircleStyle.prototype.hoverOut = function(elem) {
      return Util.removeClass(elem, this.hoverClass);
    };

    CircleStyle.prototype.isSimilar = function(elem) {
      return Util.hasClass(elem, this.similarClass);
    };

    CircleStyle.prototype.similarOn = function(elem) {
      return Util.addClass(elem, this.similarClass);
    };

    CircleStyle.prototype.similarOff = function(elem) {
      return Util.removeClass(elem, this.similarClass);
    };

    CircleStyle.prototype.hide = function(elem) {
      return Util.addClass(elem, this.hiddenClass);
    };

    CircleStyle.prototype.show = function(elem) {
      return Util.removeClass(elem, this.hiddenClass);
    };

    CircleStyle.prototype.isTagged = function(elem) {
      return Util.hasClass(elem, this.taggedClass);
    };

    CircleStyle.prototype.taggedOn = function(elem) {
      var hashcode;
      if (!Util.hasClass(elem, this.taggedClass)) {
        hashcode = randomString(8);
        this.appendCover(elem, this.taggedCoverClass, "tagged-" + hashcode);
        Util.addClass(elem, this.taggedClass);
        return elem.setAttribute("data-tagged-target", "tagged-" + hashcode);
      }
    };

    CircleStyle.prototype.taggedOff = function(elem) {
      var celem, i, len, ref, results;
      Util.removeClass(elem, this.taggedClass);
      ref = document.querySelectorAll('.' + this.taggedCoverClass);
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        celem = ref[i];
        results.push(celem.remove());
      }
      return results;
    };

    CircleStyle.prototype.taggedSimilarOn = function(elem) {
      if (!Util.hasClass(elem, this.taggedClass)) {
        return Util.addClass(elem, this.taggedSimilarClass);
      }
    };

    CircleStyle.prototype.taggedSimilarOff = function(elem) {
      return Util.removeClass(elem, this.taggedSimilarClass);
    };

    CircleStyle.prototype.cleanClick = function() {
      var elem, i, len, ref, results;
      ref = document.querySelectorAll('.' + this.clickClass);
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        elem = ref[i];
        results.push(this.clickOff(elem));
      }
      return results;
    };

    CircleStyle.prototype.cleanHover = function() {
      var elem, i, len, ref, results;
      ref = document.querySelectorAll('.' + this.hoverClass);
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        elem = ref[i];
        results.push(this.hoverOut(elem));
      }
      return results;
    };

    CircleStyle.prototype.cleanSimilar = function() {
      var elem, i, len, ref, results;
      ref = document.querySelectorAll('.' + this.similarClass);
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        elem = ref[i];
        results.push(this.similarOff(elem));
      }
      return results;
    };

    CircleStyle.prototype.cleanHidden = function() {
      var elem, i, len, ref, results;
      ref = document.querySelectorAll('.' + this.hiddenClass);
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        elem = ref[i];
        results.push(this.show(elem));
      }
      return results;
    };

    CircleStyle.prototype.cleanTagged = function() {
      var elem, i, len, ref, results;
      ref = document.querySelectorAll('.' + this.taggedClass);
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        elem = ref[i];
        results.push(this.taggedOff(elem));
      }
      return results;
    };

    CircleStyle.prototype.cleanTaggedSimilar = function() {
      var elem, i, len, ref, results;
      ref = document.querySelectorAll('.' + this.taggedSimilarClass);
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        elem = ref[i];
        results.push(this.taggedSimilarOff(elem));
      }
      return results;
    };

    CircleStyle.prototype.cleanAll = function() {
      this.cleanClick();
      this.cleanHover();
      this.cleanSimilar();
      return this.cleanHidden();
    };

    CircleStyle.prototype.appendCover = function(elem, klass, hashcode) {
      var node, parent, position;
      if (parent = elem.parentNode) {
        position = Util.position(elem);
        if (position) {
          node = document.createElement("div");
          node.className = klass;
          node.style.width = Util.width(elem) + 'px';
          node.style.height = Util.height(elem) + 'px';
          node.style.left = position.left + 'px';
          node.style.top = position.top + 'px';
          if (hashcode) {
            node.setAttribute("data-orig", hashcode);
          }
          while ((parent != null) && ['TABLE', 'TR', 'TD', 'TH'].indexOf(parent.tagName) !== -1) {
            parent = parent.parentNode;
          }
          if (parent) {
            return parent.appendChild(node);
          }
        }
      }
    };

    return CircleStyle;

  })();
  function randomString(length) {
    var chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz'.split('');

    if (!length) {
      length = Math.floor(Math.random() * chars.length);
    }

    var str = '';
    for (var i = 0; i < length; i++) {
      str += chars[Math.floor(Math.random() * chars.length)];
    }
    return str;
  }


function getPageTags(tags, page) {
  var domain = page.domain;
  var path = page.path;
  var query = page.query;
  var pageTags = tags.filter(function(t) {
    var filterDomain = t.filter.domain;
    var filterPath = t.filter.path;
    var filterQuery = t.filter.query;
    return t.eventType === "elem" &&
      (typeof(filterDomain) === "undefined" ||
       ((filterDomain.indexOf('*') === -1) ? (filterDomain === domain) : domain.match(filterDomain.replace("*", ".*")))) &&
      (typeof(filterPath) !== "undefined" &&
       (filterPath !== '*' && filterPath !== '/*' &&
        ((filterPath.indexOf('*') === -1) ? (filterPath === path) : path.match(filterPath.replace("*", ".*"))))) &&
      (typeof(filterQuery) === "undefined" || filterQuery === query);
    });

    return (pageTags.length > 0 ? pageTags.sort(function (a,b) {
      if (typeof(a.filter.path) === "undefined" || a.filter.path === '*' || a.filter.path[a.filter.path.length-1] === '*') {
        return 1;
      } else if (typeof(b.filter.path) === "undefined" || b.filter.path === '*' || b.filter.path[b.filter.path.length-1] === '*') {
        return -1;
      } else {
        return a.filter.path > b.filter.path;
      }
    }) : []);
  }

  var _vds_hybrid = window._vds_hybrid;
  if (_vds_hybrid) {
    var sty = document.createElement("style");
    sty.innerHTML = ".growing-circle-tagged-cover { position: absolute; border: 1px solid #FFDD24; border-radius: 3px; background-color: rgba(255, 221, 36, 0.3); z-index: 999999;}";
    document.head.appendChild(sty);
    _vds_hybrid.tagStore = new TagStore();
    _vds_hybrid.toggleEye = new ToggleEye(_vds_hybrid.tagStore, new CircleStyle());
    _vds_hybrid.toggleEye.toggled = false;

    _vds_hybrid.setTags = function (tags) {
      var showing = _vds_hybrid.toggleEye.toggled;
      _vds_hybrid.toggleEye.off();
      var currentCovers = document.querySelectorAll("div.growing-circle-tagged-cover");
      for (var i = currentCovers.length - 1; i >= 0; i--) {
        currentCovers[i].parentNode.removeChild(currentCovers[i]);
      }
      if (tags) {
        _vds_hybrid.tagStore.tags = {};
        tags = getPageTags(tags, {domain: location.host, path: location.pathname, query: Util.query()});
        for (var i = tags.length - 1; i >= 0; i--) {
          _vds_hybrid.tagStore.add(tags[i]);
        }
        if (showing) {
          _vds_hybrid.toggleEye.on();
        }
      }
    }

    _vds_hybrid.setShowCircledTags = function (show) {
      var lastState = _vds_hybrid.toggleEye.toggled;
      if (show) {
        if (!_vds_hybrid.isMoving) _vds_hybrid.toggleEye.on();
      } else {
        _vds_hybrid.toggleEye.off();
      }
      return lastState;
    }
    console.log('start');
  }
}).call(this);