//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.parser;

import jd.http.Browser;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public final class JavaScript {
    public String javaScript;

    public JavaScript(String javaScript) {
        this.javaScript = javaScript;
    }

    public String callFunction(String functionName) {
        return callFunction(functionName, null);
    }

    public String callFunction(String functionName, String[] parameters) {

        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        StringBuilder parameter = new StringBuilder();
        if (parameters != null) {
            if (parameters.length > 0) {
                parameter.append('\'');
                parameter.append(parameters[0]);
                parameter.append('\'');
                for (int i = 1; i < parameters.length; i++) {
                    parameter.append(new char[] {',',' ', '\''});
                    parameter.append(parameters[i]);
                    parameter.append('\'');
                }
            }
        }
        String fun = "function f(){ " + javaScript + "\nreturn " + functionName + "(" + parameter.toString() + ")} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        String ret = Context.toString(result);
        Context.exit();
        return ret;
    }

    public String runJavaScript() {
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        Object result = cx.evaluateString(scope, javaScript, "<cmd>", 1, null);
        String ret = Context.toString(result);
        Context.exit();
        return ret;
    }

    public String toString() {
        // TODO Auto-generated method stub
        return javaScript;
    }

    /**
     * TODO muss noch überarbeitet werden
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            Browser b = new Browser();
            b.getPage("http://rapidlayer.in/go/c8f16ccc");
            JavaScript js = b.getJavaScript();
            js.javaScript = "var document;\r\n" + js.javaScript;
            String fun_id = b.getRegex("function (.*?)\\(").getMatch(0);
            String dec = b.getRegex("a=(.*?);document.write").getMatch(0);

            // js.javaScript=js.javaScript.replaceAll(
            // "document.getElementById\\('ausgabe'\\).innerHTML=", "return ");
            System.out.println(js);
            System.out.println(js.callFunction(fun_id, new String[] { dec }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
