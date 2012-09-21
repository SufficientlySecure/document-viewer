/*
 * This script features the following
 * - It creates the "Language select" menu in the sidebar
 *   which sets &uselang= and saves choise in a cookie
 * - It makes suggestions on urls without &uselang  through
 *   "View Wikimedia Commons in <language>" notices (suggestion
 *   data comes from cookies, referal url & browser settings)
 * - Regardless of suggestions and cookies, if your current url
 *   includes a &uselang=, when you click a link, it will append uselang
 *   to the next page you visit (unless that url already has a &uselang harcoded)
 */

// Don't load twice, don't load in skins other than the default
if ( skin === 'vector' && typeof AnonymousI18N === 'undefined' && typeof $ !== 'undefined' ) {

	// Hook for tools to suggest a language based on another system
	// Commented out, other scripts may set it at any point in time
	// (before this script loads). 
	// This commented-out declaration is here as a reminder.
	//window.AnonymousI18N_suggestlang = null; 

	/**
	 * AnonymousI18N.js
	 *
	 * Suggests, presents, stores and applies uselang for anonymous users
	 * Only sets a cookie if needed (when suggestion notice or dropdown is clicked)
	 * If no cookie is set but ?uselang= is set it maintains that uselang but doesn't save anything
	 *
	 * @created: December 6th, 2010
	 * @stats: [[File:Krinkle_AnonymousI18N.js]]
	 * @author: User:Krinkle <krinkle (at) gmail (dot) com>
	 * @license: Triple licensed Creative Commons Attribution 3.0[1], GFDL[2] and GPL[3]
	 *
	 * [1] <http://creativecommons.org/licenses/by-sa/3.0/>
	 * [2] <http://www.gnu.org/licenses/fdl.html>
	 * [3] <http://www.gnu.org/copyleft/gpl-3.0.html>
	 */
	window.AnonymousI18N = {

		// Current revision, used for development, cache clearing and tracking
		rev: 'r16',

		// Name of the cookie that stores the language of choise
		cookie_lang: 'AnonymousI18N_lang',

		// Name of the cookie that stores wether the user has declined the notice
		cookie_decline: 'AnonymousI18N_decline',

		// Number of days new cookie's get as expiration
		cookie_expiration: 10,

		// Page with more documentation
		documentation : 'http://meta.wikimedia.org/wiki/User:Krinkle/Scripts/AnonymousI18N',

		/* Dear editors that add/remove/modify translations below,
		 *
		 * README:
		 * Please take great care to the commas.
		 * There should be a comma *after each definition* except for the *last entry*.
		 * This means if you remove the last line you have to remove the comma from the, now last, definition.
		 * This means if you add a line to the bottom that this line should NOT end in a comma
		 *    and the former last line should get a comma to the end ! 
		 */
		// User interface messages
		msgHelp: {
			'ca': 'Llengua',
			'cs': 'Výběr jazyka',
			'da': 'Vælg sprog',
			'de': 'Sprachauswahl',
			'en': 'Language select',
			'eo': 'Lingvoelekto',
			'es': 'Seleccionar idioma',
			'et': 'Keele valimine',
			'fa': '\u0627\u0646\u062A\u062E\u0627\u0628 \u0632\u0628\u0627\u0646',
			'fi': 'Valitse kieli',
			'fr': 'Sélecteur de langue',
			'ko': '언어 선택',
			'mk': 'Јазик',
			'ml': 'ഭാഷ തിരഞ്ഞെടുക്കുക',
			'nds': 'Spraakutwahl',
			'nl': 'Taal',
			'pl': 'Wybierz język',
			'pt': 'Seleção do idioma',
			'ru': 'Выбор языка',
			'sv': 'Välj språk'
		},
		msgSelect: {
			'ca': 'Selecciona',
			'cs': 'Vybrat',
			'da': 'Vælg',
			'de': 'Auswählen',
			'en': 'Select',
			'eo': 'Elekti',
			'es': 'Escoger',
			'et': 'Vali',
			'fa': '\u0627\u0646\u062A\u062E\u0627\u0628',
			'fi': 'Valitse',
			'fr': 'Valider',
			'ko': '선택',
			'mk': 'Одбери',
			'ml': 'തിരഞ്ഞെടുക്കുക',
			'nds': 'Utwählen',
			'nl': 'Selecteer',
			'pl': 'Wybierz',
			'pt': 'Selecionar',
			'ru': 'Выбрать',
			'sv': 'Välj'
		},
		msgReset: {
			'ca': 'Anul·la',
			'cs': 'Vymazat paměť',
			'en': 'Reset language',
			'de': 'Auswahl zurücksetzen',
			'es': 'Olvidar',
			'et': 'Puhasta mälu',
			'fa': '\u067E\u0627\u06A9\u200C\u06A9\u0631\u062F\u0646 \u062D\u0627\u0641\u0638\u0647',
			'fi': 'Unohda valinta',
			'fr': 'Réinitialiser',
			'nl': 'Geheugen wissen',
			'pt': 'Limpar a memória',
			'sv': 'Rensa minnet'
		},
		msgSuggest : {  // $1 = site name, $2 = link containing language name (see below)
			'ca' : 'Vegeu $1 en $2',
			'cs' : '$1 je dostupné $2',
			'da' : 'Se $1 på $2',
			'de' : '$1 auf $2',
			'en' : 'View $1 in $2',
			'es' : '$1 está disponible en $2',
			'et' : 'Kuva $1 $2.',
			'fa' : '$1 \u062F\u0631 $2 \u0628\u0628\u06CC\u0646\u06CC\u062F',
			'fi' : '$1 on käytettävissä $2.',
			'fr' : '$1 est disponible $2.',
			'ja' : 'ウィキメディアコモンズを$2で表示',
			'ko' : '$1 $2 로 보기',
			'nl' : '$1 in het $2',
			'pl' : '$1 można przeglądać $2',
			'pt' : '$1 está disponível em $2.',
			'ru' : '$1 доступен $2',
			'sv' : 'Visa $1 på $2'
		},
		// If the plain language name as shown in the select box doesn't work for the suggestion box link,
		// ("$2" above), you may specify a custom link text below.  XXX: The link text does _not_ fall back
		// from "aa-bb" to "aa", but instead to the language/dialect name from wpSupportedLanguages.  This
		// means that if you add custom link texts for any languages that have dialects supported by
		// MediaWiki, you'll probably need to add them for the dialects too.
		msgSuggestLink : {
			'ca' : 'català',
			'cs' : 'v češtině',
			'et': 'eesti keeles',
			'fi' : 'suomeksi',
			'fr' : 'en français',
			'pl' : 'w języku polskim',
			'ru' : 'на русском языке',
			'sv' : 'svenska'
		},

		// Get user language
		// Priority:
		// 1. Cookie
		// 2. Hook (optional)
		// 3. Browser language
		// 4. Default user language (fallback)
		getUserLanguage : function() {

			// Get cookie value
			var language = $.cookie( this.cookie_lang );

			// If there's a different script providing suggestions through the hook, use it
			if ( !language && typeof AnonymousI18N_suggestlang === 'string' ) {
				language = AnonymousI18N_suggestlang;
			}

			// Get user language (uselang=)
			if ( !language && wgUserLanguage !== wgContentLanguage ) {
				language = wgUserLanguage;
			}

			// Get browser language
			if ( !language ) {
				language = navigator.userLanguage || navigator.language || navigator.browserLanguage;
			}

			// Convert to lower case
			language = language.toLowerCase();

			// Check if the (possibly dashed) code is an available language code
			if ( typeof wpAvailableLanguages[language] === 'string' ) {
				return language;
			} else {
				// convert 'aa-BB' to 'aa' etc.
				language = language.split("-")[0];
				if ( typeof wpAvailableLanguages[language] === 'string' ) {
					return language;
				} else {
					// The extracted code isn't available, fallback to default
					return wgUserLanguage;
				}
			}

		},

		getLangDropdown : function() {
			// Begin dropdown
			var dropdownHTML = '<select name="mw-AnonymousI18N-picker" id="mw-AnonymousI18N-picker">';

			// Add option for each language
			$.each( wpAvailableLanguages, function( key, val ) {
				// using jQuery(...).attr().text().html() which properly escapes special characters in inner text and attribute values
				// becuase language titles could contain apostrophes and/or need-htmlentitity-escape characters
				if ( key === wgUserLanguage ) {
					dropdownHTML += $( '<option selected="selected">' ).attr( 'value', key ).text( val ).wrap( '<div>' ).parent().html();
				} else {
					dropdownHTML += $( '<option>' ).attr( 'value', key ).text( val ).wrap( '<div>' ).parent().html();
				}
			} );

			// End dropdown
			var is_cookie_set = !!$.cookie( this.cookie_lang );
			dropdownHTML += '</select><input type="button" name="mw-AnonymousI18N-select" id="mw-AnonymousI18N-select" value="' + (this.msgSelect[wgAnonUserLanguage] || this.msgSelect[wgAnonUserLanguage.split("-")[0]] || this.msgSelect.en) + '"></input>' + ( is_cookie_set ? '<br /><a id="mw-AnonymousI18N-reset" href="#">' + (this.msgReset[wgAnonUserLanguage] || this.msgReset[wgAnonUserLanguage.split("-")[0]] || this.msgReset.en) + '</a>' : '' );

			return dropdownHTML;
		},

		// Redirect location to another language of the current page
		redirectCurrentPageToLanguage : function( lang ) {

			// If there is no lang passed, redirect to uselang-less page
			if ( !lang ) {
				location.href = location.protocol + '//' + location.host + location.pathname;
				return true;
			}

			// If there is no query string yet, create one
			if ( location.search === '' ) {
				location.href = location.protocol + '//' + location.host + location.pathname + '?uselang=' + encodeURIComponent( lang ) + location.hash;

			// There is a query string already
			} else {

				// If there's a uselang= already, replace it
				if ( location.search.indexOf('uselang=') !== -1 ) {
					var search = location.search.split( 'uselang=' + encodeURIComponent( wgUserLanguage ) ).join( 'uselang=' + encodeURIComponent( lang ) );
					location.href = location.protocol + '//' + location.host + location.pathname + search + location.hash;

				// Otherwise, add it
				} else {
					location.href = location.protocol + '//' + location.host + location.pathname + location.search + '&uselang=' + encodeURIComponent( lang ) + location.hash;
				}
			}
		},

		// Redirect location to another language of a given url
		getUrlForLanguage : function( url, lang ) {

			// Cut out hash if present
			if ( url.indexOf( '#' ) !== -1 ) {
				url = url.substr( 0, url.indexOf( '#' ) );
			}

			// Check if URL contains query string
			var m = url.match(/\?[^#]*/);
			if ( m !== null && m[0] !== null ) {

				// There is a query string already
				if ( url.indexOf('uselang=') !== -1 ) {
					// If there's a uselang= already, replace it
					return url.split( 'uselang=' + encodeURIComponent( wgUserLanguage ) ).join( 'uselang=' + encodeURIComponent( lang ) );
				} else {
					// Otherwise, add it
					return url + '&uselang=' + encodeURIComponent( lang );
				}
			} else {
				// If there is no query string yet, create one
				return url + '?uselang=' + encodeURIComponent( lang );
			}
		},

		init : function() {

			window.wgAnonUserLanguage = this.getUserLanguage();

			// If this language is suggested by special means (e.g. AnonymousI18N_suggestlang), then
			// redirect to that language now.
			// However since the language suggestion/override came from AnonymousI18N_suggestlang, we 
			// should not set a cookie, because the user may have already made a (different) language choice and
			// is simply following an interwiki link to Commons. In this case AnonymousI18N preserves the
			// 'uselang'-parameter without the need for a cookie.
			// Another advantage of not storing the cookie is one can have multiple browser windows open, navigating
			// their own path and preserving different languages.

			// - Temporarily disabled due to concerns, see talk page. -- Krinkle 2011-09-22
			// - - This was likely added as a work-around that is no longer needed,
			//     because redirectCurrentPageToLanguage() is called already a few lines lower in this function.
			//if ( AnonymousI18N_suggestlang && typeof wpAvailableLanguages[AnonymousI18N_suggestlang] === 'string'
			//     && wgAnonUserLanguage !== wgUserLanguage
			//) {
			//	AnonymousI18N.redirectCurrentPageToLanguage( wgAnonUserLanguage );
			//}


			// Insert Language-portal
			var portalHTML = ' <div class="portal persistent" id="p-AnonymousI18N"><h5>' + (this.msgHelp[wgAnonUserLanguage] || this.msgHelp[wgAnonUserLanguage.split("-")[0]] || this.msgHelp.en) + '</h5><div class="body">' + this.getLangDropdown() + '</div></div>';
			$( '#p-navigation' ).after( portalHTML );
			mw.util.addCSS(
				' #p-AnonymousI18N { overflow:hidden /* other browsers */; } ' +
				' #p-AnonymousI18N select { width:100% /* modern browsers */; } ' +
				' #mw-AnonymousI18N-select:hover { cursor:pointer } ' +
				' #mw-AnonymousI18N-reset { font-size: 0.6em }' );

			// Show suggestion if:
			// * the gotten language is an available language on the wiki
			// * the gotten is not the current language already
			// * there is no cookie present that represents the choise to decline suggestions
			if (	typeof wpAvailableLanguages[wgAnonUserLanguage] === 'string' && // isset()
					wgUserLanguage !== wgAnonUserLanguage &&
					$.cookie( AnonymousI18N.cookie_decline ) !== wgAnonUserLanguage + '-true' ) {

					// For the suggestion text, fall back first to undashed language code, then to English;
					// for the link, use name from wpAvailableLanguages if no custom link text is defined.
					// This may produce odd combinations like "View Wikimedia Commons in Suomi", but that's
					// still better than using the wrong language name entirely.
					var msgText = this.msgSuggest[wgAnonUserLanguage] || this.msgSuggest[wgAnonUserLanguage.split("-")[0]] || this.msgSuggest.en;
					var msgLink = this.msgSuggestLink[wgAnonUserLanguage] || wpAvailableLanguages[wgAnonUserLanguage];

					var link = $( '<a href="#" id="mw-AnonymousI18N-gosuggest"></a>' ).text( msgLink ).wrap( '<div>' ).parent().html();
					var html = msgText.replace( '$1', wgSiteName ).replace( '$2', link );

					$( '#bodyContent' ).before( '<div id="mw-AnonymousI18N-suggest" style="padding:0.3em 1em;font-size:0.8em;background:#F8F8FF;margin:0px 0px 0.5em 0px;border:1px solid #CCCCFF;text-align:center;position:relative"><small style="position:absolute;top:0.3em;right:1em">[<a href="#" id="mw-AnonymousI18N-decline">x</a>]</small> ' + html + '</div>' );

					// Bind language suggest link
					$( '#mw-AnonymousI18N-gosuggest' ).live( 'click', function() {
						$.cookie( AnonymousI18N.cookie_lang, escape( wgAnonUserLanguage ), {
							expires: AnonymousI18N.cookie_expiration,
							path: '/'
						} );
						if ( wgAnonUserLanguage !== wgUserLanguage ) {
							AnonymousI18N.redirectCurrentPageToLanguage( wgAnonUserLanguage );
						}
						return false;
					} );

					// Bind [x]-click
					$( '#mw-AnonymousI18N-decline' ).live( 'click', function() {
						// Remove the suggestion notive
						$( '#mw-AnonymousI18N-suggest' ).remove();
						// Set a cookie to no longer suggest this language
						$.cookie( AnonymousI18N.cookie_decline, wgAnonUserLanguage + '-true', {
							expires: AnonymousI18N.cookie_expiration,
							path: '/'
						} );
						return false;
					} );
			}

			// Bind Select-click
			$( '#mw-AnonymousI18N-select' ).live( 'click', function() {

				var lang = $( '#mw-AnonymousI18N-picker' ).val();

				if ( lang !== '' ) {
					$.cookie( AnonymousI18N.cookie_lang, escape( lang ), {
						expires: AnonymousI18N.cookie_expiration,
						path: '/'
					} );
					if ( lang !== wgUserLanguage ) {
						AnonymousI18N.redirectCurrentPageToLanguage( lang );
					} else {
						$( '#mw-AnonymousI18N-suggest' ).remove();
					}
				} // else { /* alert( 'No valid language selected.' ); */ }

				return false;
			} );

			// Bind Reset-click
			$( '#mw-AnonymousI18N-reset' ).live( 'click', function() {

				$.cookie( AnonymousI18N.cookie_lang, null, {
					expires: AnonymousI18N.cookie_expiration,
					path: '/'
				} );
				$.cookie( AnonymousI18N.cookie_decline, null, {
					expires: AnonymousI18N.cookie_expiration,
					path: '/'
				} );
				AnonymousI18N.redirectCurrentPageToLanguage( false );
				return false;

			} );

			// Don't manipulate all anchor-elements
			// Instead listen for any anchor-clicks and maintain uselang when and where needed
			// This way also dynamically added links (such as tabs and toolbox-links) are accounted for
			$( 'a[href]' ).live( 'click', function( e ) {

				var	href_attr = $(this).attr('href'),

				// by going to element directlty instead of attribute the urls become complete
				// this makes '/wiki/Article' into 'http://domain.org/wiki/Article'
				// but also '#' into 'http://domain.org/wiki/Article#' !
					href_full = this.href;

				// If
				// * the link destination is a true url (ie. don't interrupt #toc links or 'javascript:call()' stuff)
				// * links to a domain within wikimedia such as http://nl.wiktionary, http://en.wikipedia but also https://secure.wikimedia.org/
				// * The choosen language is not the default (which makes uselang redundant)
				if (	href_attr.substr(0,1) !== '#' &&
						href_attr !== '' &&
						href_full.substr(0,4) === 'http' &&
						href_full.indexOf( '.wik' ) !== -1 &&
						href_full.indexOf( '.org' ) !== -1 &&
						wgUserLanguage !== wgContentLanguage ) {
					// Prevent the default linking functionalty and instead use getUrlForLanguage()
					e.preventDefault();
					location.href = AnonymousI18N.getUrlForLanguage( href_full, wgAnonUserLanguage );

					} // Else:  Don't interrupt and follow link regularly
			} );

			// Don't manipulate all form-elements
			// Listen for submit()'s and work the uselang in there
			// Works on edit pages (Save, Preview, Diff) - not for <inputbox>'es and Search (yet) though
			$( 'form' ).submit( function( e ) {
				var $el = $(this);
				$el.attr( 'action', function() {
					return AnonymousI18N.getUrlForLanguage( $el.attr( 'action' ), wgAnonUserLanguage );
				} );

			} );

			// The following are special cases that redirect partially and is ignoring GET over POST
			// For these, we need to add an input element for uselang so that it goes with the POST
			// Does not work live() but these elements aren't added dynamically so that's okay
			$( 'form.createbox, form.searchbox, form#search' ).append( $( '<input type="hidden" name="uselang" />' ).val( wgAnonUserLanguage ) );

		}
	};

	/**
	 * referrerWikiUselang
	 * @author Mdale
	 * @created December 2, 2010
	 *
	 * Makes a best guess at the user language based on the referring wiki.
	 * To get the language call: referrerWikiUselang.getReferrerLang();
	 * Returns a string with the language code or null if nothing was found in the referral.
	*/
	window.referrerWikiUselang = {
		// The supported list of projects that include language codes at the start of their url
		'projectList': [ 'wikipedia' , 'wiktionary' , 'wikinews' , 'wikibooks' , 'wikisource' , 'wikiversity' , 'wikiquote' ],

                // Some other common sites for which we happen to know its language
                'alternateSites': { 'http://www.wikiskripta.eu': 'cs'  },

		/**
		 * Checks if the language is supported
		 * @param {string} uselang The language code to check
		 * @return {boolean} true if language is supported, false if the language is not supported
		 */
		'isSupportedRewriteLanguage': function( useLang ) {
			// Check that the language is supported
			if ( !( useLang in wpAvailableLanguages ) ) {
				return false;
			}
			// Just in case someone did not see documentation above ( don't rewrite default language )
			if ( useLang === wgContentLanguage ) {
				return false;
			}
			return true;
		},
		/**
		 * Get the language code from wikimedia site the user came from
		 * @return {String} the referrer language code
		 */
		'getReferrerLang': function() {
			if ( document.referrer ) {
				// We should use mw.parseUri ( for now regEx )
				var matches = document.referrer.match(/^https?\:\/\/([^\.]*)\.([^\.]*)([^\/:]*)/);
	
				if ( matches && $.inArray( matches[2], this.projectList ) !== -1 ){
					this.referrerLang = matches[1];
					if ( this.isSupportedRewriteLanguage( this.referrerLang ) ) {
						return this.referrerLang;
					} else {
						return null;
					}
                                } else if ( matches && this.alternateSites[matches[0]] !== undefined ) {
					return this.alternateSites[ matches[0] ];
				} else {
					return null;
				}
			}
			return null;
		}
	};


	$( document ).ready( function() {
			// referrerWikiUselang could theoretically be broken, verify that it's there
			if ( referrerWikiUselang && referrerWikiUselang.getReferrerLang ) {
				// Get the langcode of the referring wiki if there is one
				// And add it as an extra suggestion in the AnonymousI18N.getUserLanguage()
				window.AnonymousI18N_suggestlang = referrerWikiUselang.getReferrerLang();
			}
			// Verify the wpAvailableLanguages is set properly, then init stuff
			if ( wpAvailableLanguages.en === 'English' ) {
				AnonymousI18N.init();
			}
	} );
}