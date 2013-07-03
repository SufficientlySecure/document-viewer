package org.emdev.ui.preference;

import org.ebookdroid.R;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import org.emdev.common.fonts.FontManager;
import org.emdev.common.fonts.data.FontFamily;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.utils.WidgetUtils;
import org.emdev.utils.enums.EnumUtils;

public class FontPickerPreference extends ListPreference {

    private final FontFamilyType type;

    public FontPickerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        final String fontFamily = WidgetUtils.getStringAttribute(context, attrs, WidgetUtils.EBOOKDROID_NS,
                WidgetUtils.ATTR_FONT_FAMILY, null);

        type = EnumUtils.getByResValue(FontFamilyType.class, fontFamily, null);

        final List<String> values = new ArrayList<String>();
        final List<String> entries = new ArrayList<String>();

        if (type != null) {
            final String suffix = ", " + type.getResValue();

            values.add("");
            entries.add(context.getString(R.string.pref_systemfontpack) + suffix);

            for (final FontPack fp : FontManager.external) {
                final FontFamily family = fp.getFamily(type);
                if (family != null) {
                    values.add(fp.name);
                    entries.add(fp.name + suffix);
                }
            }
        } else {
            for (final FontPack fp : FontManager.system) {
                for (FontFamily family : fp) {
                    if (family.type != FontFamilyType.SYMBOL && family.type != FontFamilyType.DINGBAT) {
                        values.add(fp.name + ", " + family.toString());
                        entries.add(fp.name + ", " + family.toString());
                    }
                }
            }

            for (final FontPack fp : FontManager.external) {
                for (final FontFamily family : fp) {
                    if (family.type != FontFamilyType.SYMBOL && family.type != FontFamilyType.DINGBAT) {
                        values.add(fp.name + ", " + family.toString());
                        entries.add(fp.name + ", " + family.toString());
                    }
                }
            }
        }

        setEntries(entries.toArray(new CharSequence[entries.size()]));
        setEntryValues(values.toArray(new CharSequence[values.size()]));
    }
}
