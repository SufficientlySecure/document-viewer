/**
 * to benefit of [[:Template:Multilingual description]]
 *
 * Implements language selection for multilingual elements
 *
 * WARNING: DON'T RELY ON COMMONS-STUFF. THIS SCRIPT IS FOREIGN-USED
 * THE SCRIPT IS ALSO USED BY SOME GADGETS THAT LOAD CONTENT DYNAMICALLY
 *
 * In certain environments, it's not feasible to neatly box away each
 * different language into its own section of the site. By marking elements
 * multilingual, you can emulate this behavior by only displaying the
 * message in the user's language. This reduces the "Tower of Babel" effect.
 *
 * @author Edward Z. Yang (Ambush Commander), Rewritten by DieBuche
 */

/*global mediaWiki:false, jQuery:false, wpAvailableLanguages:false*/
/*jshint curly:false*/

(function($, mw) {
'use strict';

var multilingual, commonsUserLanguage = mw.config.get('wgUserLanguage');
multilingual = window.multilingual = {
	/* Configuration: */
	
	// the cookie name we use to stash the info.
	cookie: 'commonswiki_language_js',

	// link to the language select page
	helpUrl: '//meta.wikimedia.org/wiki/Meta:Language_select',
	
	// how many languages are required to collapse
	langCountThreshold: 4,
	
	// The element that's children should be checked
	$p: $('#mw-content-text'),
	
	// How to add the selector?
	method: 'prepend',
	
	// To which element should the selector be added (reset in $(document).ready())
	$OuterContainer: mw.util.$content,

	// strings that are part of the widgets
	stringHelp: {
		'be-tarask': 'Выбар мовы',
		'be-x-old': 'Выбар мовы',
		'cs': 'Výběr jazyka:',
		'da': 'Vælg sprog:',
		'de': 'Sprachauswahl:',
		'en': 'Language select:',
		'eo': 'Lingvoelekto:',
		'fr': 'Sélecteur de langue&nbsp;:',
		'hu': 'Nyelvválasztás:',
		'ko': '언어 선택:',
		'mk': 'Јазик:',
		'ml': 'ഭാഷ തിരഞ്ഞെടുക്കുക:',
		'nds': 'Spraakutwahl:',
		'nl': 'Taal:',
		'pl': 'Wybierz język:',
		'pt': 'Seleção do idioma:',
		'pt-br': 'Seleção do idioma:',
		'ru': 'Выбор языка:',
		'sv': 'Välj språk:'
	},
	stringShowAll: {
		'be-tarask': 'Паказаць усе',
		'be-x-old': 'Паказаць усе',
		'cs': 'Zobrazit všechny',
		'da': 'Vis alle',
		'de': 'Alle anzeigen',
		'en': 'Show all',
		'eo': 'ĉiuj',
		'fr': 'Toutes les langues',
		'hu': 'Mutasd mind',
		'ko': '모두 보기',
		'mk': 'Сите',
		'ml': 'എല്ലാം',
		'nds': 'All wiesen',
		'nl': 'Toon alles',
		'pl': 'Pokaż wszystkie',
		'pt': 'Mostrar todos',
		'pt-br': 'Mostrar todos',
		'ru': 'Показать все',
		'sv': 'Visa alla'
	},

	/* Code: */

	// autodetects a browser language
	getBrowserLanguage: function () {
		return navigator.userLanguage || navigator.language || navigator.browserLanguage;
	},

	// sets a new language to the cookie
	setCookieLanguage: function (language) {
		$.cookie(this.cookie, language, {
			expires: 100,
			path: '/'
		});
	},

	// deletes the cookie
	deleteCookieLanguage: function (language) {
		$.cookie(this.cookie, null, {
			path: '/'
		});
	},
	// grabs the ISO 639 language code based
	// on either the browser or a supplied cookie
	getLanguage: function () {
		var language = '';

		// Priority:
		//  1. Cookie
		//  2. wgUserLanguage global variable
		//  3. Browser autodetection
		// grab according to cookie
		language = $.cookie(this.cookie);

		// grab according to wgUserLanguage if user is logged in
		if (!language && commonsUserLanguage && !mw.user.anonymous()) {
			language = commonsUserLanguage;
		}

		// grab according to browser if none defined
		if (!language) language = this.getBrowserLanguage();

		// inflexible: can't accept multiple languages
		// remove dialect/region code, leaving only the ISO 639 code
		// language = language.replace(/(-.*)+/, '');
		return language;
	},

	// build widget for changing the language cookie
	buildWidget: function (language) {

		this.$container = $('<div/>');
		// link to language select description page
		this.$container.html('<a href="' + this.helpUrl + '" class="ls_link">' + this.stringHelpText + '</a> ');
		this.$select = $('<select/>');


		var seen = {};
		this.mls.find('[lang]').each(function () {
			var lang = $(this).attr('lang');
			if (!seen[lang]) {
				seen[lang] = true;
				var verboseLang = lang;
				if (window.wpAvailableLanguages) verboseLang = wpAvailableLanguages[lang] || lang;
				multilingual.$select.append($('<option>', { text: verboseLang, value: lang }));
			}
		});
		this.$select.prepend('<option value="showall">' + this.stringShowallText + '</option>');
		this.$select.attr('id', 'langselector');
		this.$select.val(this.getLanguage());
		this.$select.change(function () {
			multilingual.setCookieLanguage($('#langselector').val());
			multilingual.apply($('#langselector').val());
		});
		
		this.$container.append(this.$select);
		// Finally insert the select using the specified method to the specified OuterContainer to the DOM-tree
		this.$OuterContainer[this.method](this.$container);
	},

	// main body of the function
	init: function () {
		if (typeof ls_enable !== 'undefined') return;

		//disabling the gadget on special pages
		if (mw.config.get('wgNamespaceNumber') < 0) return;

		// only activated in view , purge, historysubmit or submit mode
		if (-1 === $.inArray(mw.config.get('wgAction'), ['view', 'purge', 'edit', 'historysubmit', 'submit'])) return;
		
		// Fire an event just before doing anything: This allows gadgets to first adjust the variables and then run it
		$(document).triggerHandler('scriptLoaded', ['Multilingual description', multilingual]);
		
		var language = this.getLanguage();
		this.stringHelpText = (this.stringHelp[language] || this.stringHelp[commonsUserLanguage.split('-')[0]] || this.stringHelp.en);
		this.stringShowallText = (this.stringShowAll[language] || this.stringShowAll[commonsUserLanguage.split('-')[0]] || this.stringShowAll.en);

		//try to find loose {{en|...}} and wrap them in a .multilingual
		//Select all blockelement.description[lang] which have at least one of their kind preceding them.
		var blockElements = ['div', 'table'];
		$.each(blockElements, function(i, be) {
			multilingual.$p.find(be + '.description[lang]+' + be + '.description[lang]').each(function () {
				var $this = $(this);
				//Already in a multiling, nothing to do here;
				if ($this.parent().hasClass('multilingual')) return;
				var group = $(this).prevUntil(':not(' + be + '.description[lang])').add($(this)).add($(this).nextUntil(':not(' + be + '.description[lang])'));
				//Check how many associating language <blocks> exist
				if (group.length < multilingual.langCountThreshold) {
					return true;
				}
				group.wrapAll('<div class="multilingual"></div>');
			});
		});

		// grab an array of multilingual elements
		this.mls = multilingual.$p.find('.multilingual');

		// Only build form if there are MLDs on page.
		if (!this.mls.length) return;

		this.buildWidget();
		this.apply(this.getLanguage());
	},

	apply: function (language) {
		// if language is blank, delete the cookie and then recalculate
		if (!language) {
			this.deleteCookieLanguage();
			language = this.getLanguage();
		}

		this.mls.each(function () {
			// Cache selector
			var $ml = $(this);

			if ($ml.parent('[class^="image_annotation_content"]').length) return true;

			var id = $ml.attr('id');
			if (id === 'bodyContent' || id === 'wikiPreview' || id === 'LangTableLangs') return true;

			var $reqLang = $ml.find('[lang="' + language + '"]');

			if ($reqLang.length) {
				$ml.children('[lang][lang!="' + language + '"]').hide();
				$ml.children('[lang="' + language + '"]').show();
			} else {
				$ml.children('[lang]').show();
			}
		});
	}
};

// Since https://bugzilla.wikimedia.org/show_bug.cgi?id=32537 
// is fixed we should be allowed to depend on 'site' (what we need for wpAvailableLanguages): , 'site'
mw.loader.using(['jquery.cookie', 'mediawiki.user', 'mediawiki.util'], function () {
	$(document).ready(function () {
		if ($('#file').length) {
			multilingual.method = 'append';
			multilingual.$OuterContainer = $('#file');
		}
		else {
			multilingual.method = 'prepend';
			multilingual.$OuterContainer = mw.util.$content;
		}
		multilingual.init();
	});
});

})(jQuery, mediaWiki);