//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.parser.html;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.parser.Regex;

import org.appwork.utils.StringUtils;

public class InputField {
    public static InputField parse(String data) {
        return InputField.parse(data, -1, null);
    }

    protected static InputField parse(String data, final long timestamp, List<String> values) {
        // lets make all quotation marks within 'data' the same. As it's hard to make consistent regex 'matches' when quote marks are not
        // the same, without using lazy regex!.
        ArrayList<String> cleanupRegex = new ArrayList<String>();
        cleanupRegex.add("(\\w+\\s*=\\s*\"[^\"]*\")");
        cleanupRegex.add("(\\w+\\s*=\\s*'[^']*')");
        for (String reg : cleanupRegex) {
            String results[] = new Regex(data, reg).getColumn(0);
            if (results != null) {
                String quote = new Regex(reg, "(\"|')").getMatch(0);
                for (String result : results) {
                    String cleanedResult = result.replaceFirst(quote, "\\\"").replaceFirst(quote + "$", "\\\"");
                    data = data.replace(result, cleanedResult);
                }
            }
        }
        // no longer have to worry about 'data' with miss matched quotation marks!
        // Note: input form correction for 'checked' and 'disabled' fields.
        // 'disabled' can be for any input field type. Can not be changed! Value shouldn't been submitted with form, .:. null value.
        // 'checked' states current value, can can be re-sent with current request. .:. null value.
        // when 'checked' not present value shouldn't be sent/set within forms input field.
        boolean cbr = false;
        boolean checked = false;
        boolean disabled = false;
        ArrayList<String> matches = new ArrayList<String>();
        matches.add("\\s?+type\\s?+=\\s?+\"?(checkbox|radio)?\"");
        matches.add("\\s+(checked)\\s?+");
        matches.add("\\s+(disabled)\\s?+");
        for (String reg : matches) {
            String result = new Regex(data, reg).getMatch(0);
            if (result != null && result.matches("(?i)disabled")) {
                disabled = true;
            }
            if (result != null && result.matches("(?i)checked")) {
                checked = true;
            }
            if (result != null && result.matches("(?i)checkbox|radio")) {
                cbr = true;
            }
        }
        ArrayList<String> input = new ArrayList<String>();
        // end of a " or ' (we corrected above so they are all ") is end of value of key, space before next key name isn't required.
        input.add("[\"']{0,1}\\s*(\\w+)\\s*=\\s*\"(.*?)\"");
        // for key and value without use of " or ', the delimiter needs to be: whitespace, end of inputfield >, and NOT ' or " since they
        // shouldn't be present. Rhetorically should not contain empty value
        // need to exclude values found in URLS, as inputfields!. Also do not overwrite a set entry with secondary regex findings. - raztoki
        // 20150128
        input.add("(?!(?:https?://)?[^\\s]+[/\\w\\-\\.\\?&=]+)[\"']{0,1}\\s*(\\w+)\\s*=\\s*([^\\s\"'>]+)");
        String type = null;
        String key = null;
        String value = null;
        final HashMap<String, String> properties = new HashMap<String, String>();
        final String valueReplacement = "VALUE-" + timestamp + "-";
        for (String reg : input) {
            String[][] results = new Regex(data, reg).getMatches();
            for (final String[] match : results) {
                if (values != null && match[1].matches("^" + valueReplacement + "\\d+$")) {
                    final int index = Integer.parseInt(match[1].substring(valueReplacement.length()));
                    if (index >= 0 && index < values.size()) {
                        match[1] = values.get(index);
                    }
                }
                if (match[0].equalsIgnoreCase("type") && type == null) {
                    type = match[1];
                } else if (match[0].equalsIgnoreCase("name") && key == null) {
                    key = InputField.formEncoding(match[1]);
                } else if (match[0].equalsIgnoreCase("value") && value == null) {
                    value = InputField.formEncoding(match[1]);
                    if (cbr) {
                        if (checked) {
                            // ret.put("<INPUTFIELD:CHECKED>", "true");
                        } else {
                            properties.put("<INPUTFIELD:CHECKED>", "false");
                            properties.put("<INPUTFIELD:TYPEVALUE>", InputField.formEncoding(match[1]));
                            value = InputField.formEncoding(null);
                        }
                    }
                    if (!disabled) {
                        // ret.put("CKBOX_RADIO_DISABLED", "false");
                    } else {
                        properties.put("<INPUTFIELD:DISABLED>", "true");
                        properties.put("<INPUTFIELD:TYPEVALUE>", InputField.formEncoding(match[1]));
                        value = InputField.formEncoding(null);
                    }
                } else {
                    properties.put(InputField.formEncoding(match[0]), InputField.formEncoding(match[1]));
                }
            }
        }
        final InputField ret = new InputField(key, value, type);
        if (properties.size() > 0) {
            ret.setProperties(properties);
        }
        return ret;
    }

    /**
     * always URLEncode form values parsed from html
     * 
     * @param str
     * @return
     */
    private static String formEncoding(final String str) {
        if (str == null) {
            return null;
        } else {
            try {
                return URLEncoder.encode(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return str;
            }
        }
    }

    private String                  key;
    private String                  value      = null;
    private HashMap<String, String> properties = null;
    private final String            type;

    private void setProperties(HashMap<String, String> properties) {
        this.properties = properties;
    }

    public InputField(final String key, final String value) {
        this(key, value, null);
    }

    public InputField(final String key, final String value, final String type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public File getFileToPost() {
        if (this.type == null || !this.type.equalsIgnoreCase("file")) {
            throw new IllegalStateException("No file post field");
        }
        return new File(this.value);
    }

    public String getKey() {
        return this.key;
    }

    /**
     * so you can rename inputfield without having to delete and re-add inputfield/put and loose properties etc.
     *
     * @since JD2
     * @author raztoki
     * @param key
     */
    public void setKey(final String key) {
        // inputfields require non null value
        if (key != null) {
            this.key = key;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj instanceof InputField) {
            final InputField o = (InputField) obj;
            return StringUtils.equals(this.getKey(), o.getKey()) && StringUtils.equals(this.getValue(), o.getValue()) && StringUtils.equals(this.getType(), o.getType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    public String getProperty(final String key, final String defValue) {
        final String ret;
        if (this.properties != null) {
            ret = this.properties.get(key);
        } else {
            ret = null;
        }
        return ret != null ? ret : defValue;
    }

    public String getType() {
        return this.type;
    }

    public String getValue() {
        return this.value;
    }

    public void setFileToPost(final File file) {
        if (this.type == null || !this.type.equalsIgnoreCase("file")) {
            throw new IllegalStateException("No file post field");
        }
        this.value = file.getAbsolutePath();
    }

    @Override
    public String toString() {
        if (this.properties != null) {
            return "Field: " + this.key + "(" + this.type + ")" + " = " + this.value + " [" + this.properties.toString() + "]";
        } else {
            return "Field: " + this.key + "(" + this.type + ")" + " = " + this.value + " []";
        }
    }

    public void setValue(String value2) {
        if (value2 != null) {
            this.value = value2.trim();
        } else {
            this.value = null;
        }
    }

    public boolean containsProperty(String key) {
        if (this.properties != null) {
            return this.properties.containsKey(key);
        }
        return false;
    }

    /**
     * returns true when Properties of a InputField contains the provided Key and Value
     *
     * @author raztoki
     * @param key
     * @param value
     * @return
     */
    public boolean containsPropertyKeyValue(final String key, final String value) {
        if (this.properties != null) {
            if (this.containsProperty(key) && value.equals(this.properties.get(key))) {
                return true;
            }
        }
        return false;
    }

    public void putProperty(String key, String value) {
        if (this.properties == null) {
            this.properties = new HashMap<String, String>();
        }
        this.properties.put(key, value);
    }
}