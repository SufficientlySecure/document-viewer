/**
 * Ajax Translations
 *
 * Translates layout templates (licenses templates)
 * See talk page for documentation.
 *
 * Maintainers: [[User:ערן]], [[User:Ilmari Karonen]], [[User:DieBuche]]
 * Last update: 2012-05-27
 */
// Avoid post-save expansion: <nowiki>
/*global jQuery:false, mediaWiki:false*/
/*jslint browser: true */
/*jshint curly:false */
(function (mw, $) {
	"use strict";

	if (typeof mw.libs.AjaxTranslations === 'object') {
		return;
	}

	var AjaxTranslations = {
		updateLangLinks: function () {
			if (typeof window.disableAjaxTranslation !== 'undefined') {
				return;
			}
			var langLinkRgx = new RegExp(
				'^(' +
					$.escapeRE(mw.config.get('wgServer')) +
					'|)' +
					$.escapeRE(mw.config.get('wgArticlePath')).replace("\\$1", '(Template:.*/[a-z]{2,3}(-[a-z]+)?)') +
					'$'
			);
			$('div.layouttemplate a, table.layouttemplate a').each(function () {
				var m = langLinkRgx.exec($(this).attr('href'));

				// No translation link, skip it
				if (m) {
					$(this).attr('title', decodeURIComponent(m[2]).replace(/_/g, ' '));
					$(this).click(function (e) {
						e.preventDefault();
						AjaxTranslations.loadAjaxTranslation(e);
					});
				}
				AjaxTranslations.state = 'Ok, ready';
			});
		},

		/** @param {jQuery.Event} e */
		loadAjaxTranslation: function (e) {
			var templateParts,
				aT = this,
				templateArgs = '',
				linkElement = e.target,
				templateName = linkElement.title;

			if (!templateName) {
				return;
			}

			templateName = templateName.replace(/[\s_]+/g, '_');
			templateParts = /^Template:(.*)\/([a-z]{2,3}(-[a-z]+)?)$/.exec(templateName);

			if (!templateParts || !templateParts.length) {
				return;
			}

			this.lastLayoutTemplate = $('[title="' + templateName.replace(/_/g, ' ') + '"]').parents('.layouttemplate');

			// try to find encoded template args, if supplied (EXPERIMENTAL)
			this.lastLayoutTemplate.find('.layouttemplateargs').each(function () {
				var args = this.title.split(/\s+/);
				args.shift();
				$.each(args, function (key, val) {
					if (!/\S/.test(val)) {
						return true;
					}
					templateArgs += '|' + decodeURIComponent(val.replace(/\+/g, ' ')).replace(/\|/g, '{{!}}');
				});
			});

			// {{urlencode:}} turns parser extension tags into garbage; we can't undo it, so we just give up if it's happened
			if (/\x7FUNIQ/.test(templateArgs)) {
				templateArgs = '';
			}

			$.ajax({
				url: mw.util.wikiScript('api'),
				type: 'POST',
				data: {
					format : 'json',
					action : 'parse',
					prop : 'text',
					text : '{{' + templateName + templateArgs + '}}',
					title : mw.config.get('wgPageName'),
					uselang : templateParts[2]
				},
				dataType: 'json',
				success: function (result) {
					if (result && result.parse && result.parse.text['*']) {
						aT.lastLayoutTemplate.replaceWith(result.parse.text['*']);
						$('.translatedTag').hide();
						aT.updateLangLinks();
					}
				}
			});

			e.preventDefault();
		}
	};

	AjaxTranslations.state = 'Waiting for DOM-ready';

	mw.loader.using(['mediawiki.util', 'jquery.mwExtension'], function () {
		$(document).ready(AjaxTranslations.updateLangLinks);
	});

	mw.libs.AjaxTranslations = AjaxTranslations;

}(mediaWiki, jQuery));
//</nowiki>