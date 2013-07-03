package org.ebookdroid.common.settings.definitions;

import static org.ebookdroid.R.string.pref_align_by_width;
import static org.ebookdroid.R.string.pref_animation_type_none;
import static org.ebookdroid.R.string.pref_autolevels_defvalue;
import static org.ebookdroid.R.string.pref_book_align_id;
import static org.ebookdroid.R.string.pref_book_animation_type_id;
import static org.ebookdroid.R.string.pref_book_autolevels_id;
import static org.ebookdroid.R.string.pref_book_contrast_id;
import static org.ebookdroid.R.string.pref_book_gamma_id;
import static org.ebookdroid.R.string.pref_book_croppages_id;
import static org.ebookdroid.R.string.pref_book_exposure_id;
import static org.ebookdroid.R.string.pref_book_firstpageoffset_defvalue;
import static org.ebookdroid.R.string.pref_book_firstpageoffset_id;
import static org.ebookdroid.R.string.pref_book_firstpageoffset_maxvalue;
import static org.ebookdroid.R.string.pref_book_firstpageoffset_minvalue;
import static org.ebookdroid.R.string.pref_book_id;
import static org.ebookdroid.R.string.pref_book_nightmode_id;
import static org.ebookdroid.R.string.pref_book_posimages_in_nightmode_id;
import static org.ebookdroid.R.string.pref_book_rotation_id;
import static org.ebookdroid.R.string.pref_book_splitpages_id;
import static org.ebookdroid.R.string.pref_book_splitpages_rtl_id;
import static org.ebookdroid.R.string.pref_book_viewmode_id;
import static org.ebookdroid.R.string.pref_contrast_defvalue;
import static org.ebookdroid.R.string.pref_contrast_maxvalue;
import static org.ebookdroid.R.string.pref_contrast_minvalue;
import static org.ebookdroid.R.string.pref_gamma_defvalue;
import static org.ebookdroid.R.string.pref_gamma_maxvalue;
import static org.ebookdroid.R.string.pref_gamma_minvalue;
import static org.ebookdroid.R.string.pref_croppages_defvalue;
import static org.ebookdroid.R.string.pref_exposure_defvalue;
import static org.ebookdroid.R.string.pref_exposure_maxvalue;
import static org.ebookdroid.R.string.pref_exposure_minvalue;
import static org.ebookdroid.R.string.pref_nightmode_defvalue;
import static org.ebookdroid.R.string.pref_posimages_in_nightmode_defvalue;
import static org.ebookdroid.R.string.pref_rotation_unspecified;
import static org.ebookdroid.R.string.pref_splitpages_defvalue;
import static org.ebookdroid.R.string.pref_splitpages_rtl_defvalue;
import static org.ebookdroid.R.string.pref_viewmode_vertical_scroll;

import org.ebookdroid.common.settings.types.BookRotationType;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.curl.PageAnimationType;

import org.emdev.common.settings.base.BooleanPreferenceDefinition;
import org.emdev.common.settings.base.EnumPreferenceDefinition;
import org.emdev.common.settings.base.IntegerPreferenceDefinition;
import org.emdev.common.settings.base.StringPreferenceDefinition;

public interface BookPreferences {

    /* =============== Book rendering settings =============== */

    StringPreferenceDefinition BOOK = new StringPreferenceDefinition(pref_book_id, 0);

    IntegerPreferenceDefinition BOOK_FIRST_PAGE_OFFSET = new IntegerPreferenceDefinition(pref_book_firstpageoffset_id,
            pref_book_firstpageoffset_defvalue, pref_book_firstpageoffset_minvalue, pref_book_firstpageoffset_maxvalue);

    BooleanPreferenceDefinition BOOK_NIGHT_MODE = new BooleanPreferenceDefinition(pref_book_nightmode_id, pref_nightmode_defvalue);

    BooleanPreferenceDefinition BOOK_NIGHT_MODE_POS_IMAGES = new BooleanPreferenceDefinition(pref_book_posimages_in_nightmode_id, pref_posimages_in_nightmode_defvalue);

    IntegerPreferenceDefinition BOOK_CONTRAST = new IntegerPreferenceDefinition(pref_book_contrast_id,
            pref_contrast_defvalue, pref_contrast_minvalue, pref_contrast_maxvalue);

    IntegerPreferenceDefinition BOOK_GAMMA = new IntegerPreferenceDefinition(pref_book_gamma_id,
            pref_gamma_defvalue, pref_gamma_minvalue, pref_gamma_maxvalue);

    IntegerPreferenceDefinition BOOK_EXPOSURE = new IntegerPreferenceDefinition(pref_book_exposure_id,
            pref_exposure_defvalue, pref_exposure_minvalue, pref_exposure_maxvalue);

    BooleanPreferenceDefinition BOOK_AUTO_LEVELS = new BooleanPreferenceDefinition(pref_book_autolevels_id, pref_autolevels_defvalue);

    BooleanPreferenceDefinition BOOK_SPLIT_PAGES = new BooleanPreferenceDefinition(pref_book_splitpages_id,
            pref_splitpages_defvalue);

    BooleanPreferenceDefinition BOOK_SPLIT_RTL = new BooleanPreferenceDefinition(pref_book_splitpages_rtl_id,
            pref_splitpages_rtl_defvalue);

    BooleanPreferenceDefinition BOOK_CROP_PAGES = new BooleanPreferenceDefinition(pref_book_croppages_id,
            pref_croppages_defvalue);

    EnumPreferenceDefinition<BookRotationType> BOOK_ROTATION = new EnumPreferenceDefinition<BookRotationType>(
            BookRotationType.class, pref_book_rotation_id, pref_rotation_unspecified);

    EnumPreferenceDefinition<DocumentViewMode> BOOK_VIEW_MODE = new EnumPreferenceDefinition<DocumentViewMode>(
            DocumentViewMode.class, pref_book_viewmode_id, pref_viewmode_vertical_scroll);

    EnumPreferenceDefinition<PageAlign> BOOK_PAGE_ALIGN = new EnumPreferenceDefinition<PageAlign>(PageAlign.class,
            pref_book_align_id, pref_align_by_width);

    EnumPreferenceDefinition<PageAnimationType> BOOK_ANIMATION_TYPE = new EnumPreferenceDefinition<PageAnimationType>(
            PageAnimationType.class, pref_book_animation_type_id, pref_animation_type_none);

}
