package org.ebookdroid.common.settings.definitions;

import org.sufficientlysecure.viewer.R;

import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.settings.types.RotationType;
import org.ebookdroid.core.curl.PageAnimationType;

import org.emdev.common.settings.base.BooleanPreferenceDefinition;
import org.emdev.common.settings.base.EnumPreferenceDefinition;
import org.emdev.common.settings.base.IntegerPreferenceDefinition;
import org.emdev.common.settings.base.StringPreferenceDefinition;

public interface BookPreferences {

    /* =============== Book rendering settings =============== */

    StringPreferenceDefinition BOOK = new StringPreferenceDefinition(R.string.pref_book_id, 0);

    IntegerPreferenceDefinition BOOK_FIRST_PAGE_OFFSET = new IntegerPreferenceDefinition(R.string.pref_book_firstpageoffset_id,
            R.string.pref_book_firstpageoffset_defvalue, R.string.pref_book_firstpageoffset_minvalue, R.string.pref_book_firstpageoffset_maxvalue);

    BooleanPreferenceDefinition BOOK_NIGHT_MODE = new BooleanPreferenceDefinition(R.string.pref_book_nightmode_id, R.string.pref_nightmode_defvalue);

    BooleanPreferenceDefinition BOOK_NIGHT_MODE_POS_IMAGES = new BooleanPreferenceDefinition(R.string.pref_book_posimages_in_nightmode_id, R.string.pref_posimages_in_nightmode_defvalue);

    BooleanPreferenceDefinition BOOK_TINT = new BooleanPreferenceDefinition(R.string.pref_book_tint_id, R.string.pref_tint_defvalue);

    IntegerPreferenceDefinition BOOK_TINT_COLOR = new IntegerPreferenceDefinition(R.string.pref_book_tint_color_id, R.string.pref_tint_color_defvalue);

    IntegerPreferenceDefinition BOOK_CONTRAST = new IntegerPreferenceDefinition(R.string.pref_book_contrast_id,
            R.string.pref_contrast_defvalue, R.string.pref_contrast_minvalue, R.string.pref_contrast_maxvalue);

    IntegerPreferenceDefinition BOOK_GAMMA = new IntegerPreferenceDefinition(R.string.pref_book_gamma_id,
            R.string.pref_gamma_defvalue, R.string.pref_gamma_minvalue, R.string.pref_gamma_maxvalue);

    IntegerPreferenceDefinition BOOK_EXPOSURE = new IntegerPreferenceDefinition(R.string.pref_book_exposure_id,
            R.string.pref_exposure_defvalue, R.string.pref_exposure_minvalue, R.string.pref_exposure_maxvalue);

    BooleanPreferenceDefinition BOOK_AUTO_LEVELS = new BooleanPreferenceDefinition(R.string.pref_book_autolevels_id, R.string.pref_autolevels_defvalue);

    BooleanPreferenceDefinition BOOK_SPLIT_PAGES = new BooleanPreferenceDefinition(R.string.pref_book_splitpages_id,
            R.string.pref_splitpages_defvalue);

    BooleanPreferenceDefinition BOOK_SPLIT_RTL = new BooleanPreferenceDefinition(R.string.pref_book_splitpages_rtl_id,
            R.string.pref_splitpages_rtl_defvalue);

    BooleanPreferenceDefinition BOOK_CROP_PAGES = new BooleanPreferenceDefinition(R.string.pref_book_croppages_id,
            R.string.pref_croppages_defvalue);

    EnumPreferenceDefinition<RotationType> BOOK_ROTATION = new EnumPreferenceDefinition<RotationType>(
            RotationType.class, R.string.pref_book_rotation_id, R.string.pref_rotation_unspecified);

    EnumPreferenceDefinition<DocumentViewMode> BOOK_VIEW_MODE = new EnumPreferenceDefinition<DocumentViewMode>(
            DocumentViewMode.class, R.string.pref_book_viewmode_id, R.string.pref_viewmode_vertical_scroll);

    EnumPreferenceDefinition<PageAlign> BOOK_PAGE_ALIGN = new EnumPreferenceDefinition<PageAlign>(PageAlign.class,
            R.string.pref_book_align_id, R.string.pref_align_by_width);

    EnumPreferenceDefinition<PageAnimationType> BOOK_ANIMATION_TYPE = new EnumPreferenceDefinition<PageAnimationType>(
            PageAnimationType.class, R.string.pref_book_animation_type_id, R.string.pref_animation_type_none);

    BooleanPreferenceDefinition BOOK_RTL= new BooleanPreferenceDefinition(R.string.pref_book_rtl_id, R.string.pref_rtl_defvalue);
}
