/*!
 * Google Analytics customizations for "Wikia"
 *
 * ! Please don't touch this file without consulting Cardinal Path.
 *
 * @preserve Copyright(c) 2012 Cardinal Path
 * @author Eduardo Cereto <ecereto@cardinalpath.com>
 *
 * @version: prod_11
 */

(function(window, undefined) {
    var possible_domains, i;
    /**
     * Main Tracker
     *
     * To be used for everything that is not advertisement
     */
    window._gaq = window._gaq || [];

    // Main Roll-up Account - UA-32129070-1
    window._gaq.push(['_setAccount', 'UA-32129070-1']); // PROD
    //window._gaq.push(['_setAccount', 'UA-32129070-2']); // DEV
    window._gaq.push(['_setSampleRate', '10']); // 10% Sampling

    if (window.wgIsGASpecialWiki) {
        // Special Wikis account - UA-32132943-1
        window._gaq.push(['special._setAccount', 'UA-32132943-1']); // PROD
        //window._gaq.push(['special._setAccount', 'UA-32132943-2']); // DEV
        window._gaq.push(['special._setSampleRate', '100']); // No Sampling
    }

    /**
     * Wrapper function to a generic _gaq push
     *
     * Has the same interface as a _gaq.push but behind the scenes it pushes
     * to both the main account and the special account.
     *
     * Note that functions pushed into _gaq must be executed only once, so we
     * treat that just in case, to avoid duplicated function calls if we
     * decide to push a function inside _gaqWikiaPush.
     *
     * eg:
     *    _gaqWikiaPush(['_trackPageview'], ['_trackEvent', 'cat', 'act']);
     *
     * @param {Array(string)[, ...]} Each Array is a command pushed to both
     *    accounts
     */
    function _gaqWikiaPush () {
      var i, spec, args = Array.prototype.slice.call(arguments);
      for (i=0; i < args.length; i++) {
        // If it's a function just push to _gaq
        if (typeof args[i] === 'function') {
          window._gaq.push(args[i]);
          continue;
        }

        // Send to Main Account
        window._gaq.push(args[i]);

        if (window.wgIsGASpecialWiki) {
          spec = args[i].slice();
          // Send to Special Wikis Account
          spec[0] = 'special.' + spec[0];
          window._gaq.push(spec);
        }
      }
    }

    // All domains that host content for wikia.
    possible_domains = ['wikia.com', 'ffxiclopedia.org', 'jedipedia.de',
     'marveldatabase.com', 'memory-alpha.org', 'uncyclopedia.org',
     'websitewiki.de', 'wowwiki.com', 'yoyowiki.org'];

    // Use one of the domains above. If none matches the tag will fallback to
    // the default which is 'auto', probably good enough in edge cases.
    for(i=0; i<possible_domains.length; i++) {
      if(document.location.hostname.indexOf(possible_domains[i]) > -1) {
        _gaqWikiaPush(['_setDomainName', possible_domains[i]]);
        break;
      }
    }

    /**** High-Priority CVs ****/
    _gaqWikiaPush(['_setCustomVar', 1, 'DBname', window.wgDBname, 3],
                  ['_setCustomVar', 2, 'ContentLanguage',
                      window.wgContentLanguage, 3],
                  ['_setCustomVar', 3, 'Hub', window.cscoreCat, 3],
                  ['_setCustomVar', 4, 'Skin', window.skin, 3],
                  ['_setCustomVar', 5, 'LoginStatus',
                      !!window.wgUserName ? 'user' : 'anon', 3]);

    /**** Medium-Priority CVs ****/
    _gaqWikiaPush(['_setCustomVar', 8, 'PageType',
                      window.wikiaPageType, 3],
                  ['_setCustomVar', 9, 'CityId', window.wgCityId, 3],
                  ['_setCustomVar', 12, 'MedusaSlot', window.wgMedusaSlot, 3]);

    // Unleash
    _gaqWikiaPush(['_trackPageview']);


    /**
     * Advertisement Tracker, pushed separatedly.
     *
     * To be used for all ad impression and click events
     */
    // Advertisment Account UA-32129071-1
    window._gaq.push(['ads._setAccount', 'UA-32129071-1']); // PROD
    //window._gaq.push(['ads._setAccount', 'UA-32129071-2']); // DEV

    // Try to use the full domain to get a different cookie domain
    window._gaq.push(['ads._setDomainName', document.location.hostname]);

    /* Ads Account customVars */
    window._gaq.push(['ads._setCustomVar', 1, 'DBname', window.wgDBname, 3],
              ['ads._setCustomVar', 2, 'ContentLanguage',
                  window.wgContentLanguage, 3],
              ['ads._setCustomVar', 3, 'Hub', window.cscoreCat, 3],
              ['ads._setCustomVar', 4, 'Skin', window.skin, 3],
              ['ads._setCustomVar', 5, 'LoginStatus',
                  !!window.wgUserName ? 'user' : 'anon', 3]);

    /**** Medium-Priority CVs ****/
    window._gaq.push(['ads._setCustomVar', 8, 'PageType',
                  window.wikiaPageType, 3],
              ['ads._setCustomVar', 9, 'CityId', window.wgCityId, 3],
              ['ads._setCustomVar', 12, 'MedusaSlot', window.wgMedusaSlot, 3]);


    /**
     * Function used by the backend to trigger advertisement events
     *
     * Will sample the advertisement hits and send them to the appropriate
     * account.
     *
     * Has the same parameters as _trackEvent.
     * eg:
     *    gaTrackAdEvent('Impression', 'Top Banner', 'AdId');
     *
     * @param {string} category Event Category.
     * @param {string} action Event Action.
     * @param {string=""} opt_value Event Label.
     * @param {number=0} opt_value Event Value. Have to be an integer.
     * @param {boolean=false} opt_noninteractive Event noInteractive.
     */
    window.gaTrackAdEvent = function(category, action, opt_label, opt_value,
                                     opt_noninteractive) {
        var args, ad_hit_sample = 1; //1%
        if (Math.random() * 100 <= ad_hit_sample) {
            args = Array.prototype.slice.call(arguments);
            args.unshift('ads._trackEvent');
            try {
                window._gaq.push(args);
            }catch (e) {}
        }
    };

    /**
     * Function used by the backend to trigger non-advertisement events
     *
     * Will fire the event to Main account and Special wikis accounts
     * respectig standard GA sampling for the main.
     *
     * Has the same parameters as _trackEvent.
     * eg:
     *    gaTrackEvent('Impression', 'Top Banner', 'AdId');
     *
     * @param {string} category Event Category.
     * @param {string} action Event Action.
     * @param {string=""} opt_value Event Label.
     * @param {number=0} opt_value Event Value. Have to be an integer.
     * @param {boolean=false} opt_noninteractive Event noInteractive.
     */
    window.gaTrackEvent = function(category, action, opt_label, opt_value, 
                                   opt_noninteractive) {
        var args = Array.prototype.slice.call(arguments);
        args.unshift('_trackEvent');
        try {
            _gaqWikiaPush(args);
        } catch (e) {}
    };

}(window));

(function() {
  var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
  ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
  var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();