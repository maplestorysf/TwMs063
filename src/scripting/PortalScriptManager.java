/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import client.MapleClient;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import javax.script.ScriptException;
import server.MaplePortal;
import tools.FilePrinter;
import tools.StringUtil;

public class PortalScriptManager {

    private static final PortalScriptManager instance = new PortalScriptManager();
    private final Map<String, PortalScript> scripts = new HashMap();
    private final static ScriptEngineFactory sef = new ScriptEngineManager().getEngineByName("nashorn").getFactory();

    public final static PortalScriptManager getInstance() {
        return instance;
    }

    private final PortalScript getPortalScript(final String scriptName) {
        if (scripts.containsKey(scriptName)) {
            return scripts.get(scriptName);
        }

        final File scriptFile = new File("scripts/portal/" + scriptName + ".js");
        if (!scriptFile.exists()) {
            scripts.put(scriptName, null);
            return null;
        }

        InputStreamReader fr = null;
        final ScriptEngine portal = sef.getScriptEngine();
        try {
            fr = new InputStreamReader(new FileInputStream(scriptFile), StringUtil.codeString(scriptFile));
            CompiledScript compiled = ((Compilable) portal).compile(fr);
            compiled.eval();
        } catch (final FileNotFoundException | UnsupportedEncodingException | ScriptException e) {
            System.err.println("Error executing Portalscript: " + scriptName + ":" + e);
            FilePrinter.printError("PortalScriptManager.txt", e);
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException ignore) {
            }
        }
        final PortalScript script = ((Invocable) portal).getInterface(PortalScript.class);
        scripts.put(scriptName, script);
        return script;
    }

    public final void executePortalScript(final MaplePortal portal, final MapleClient c) {
        final PortalScript script = getPortalScript(portal.getScriptName());
        if (c != null && c.getPlayer() != null && c.getPlayer().hasGmLevel(2)) {
            c.getPlayer().dropMessage("您已經建立與傳送門腳本: " + portal.getScriptName() + ".js 的關聯。");
        }
        if (script != null) {
            try {
                script.enter(new PortalPlayerInteraction(c, portal));
            } catch (Exception e) {
                System.err.println("進入傳送腳本失敗: " + portal.getScriptName() + ":" + e);
            }
        } else {
            System.err.println("未處理的傳送腳本 " + portal.getScriptName() + " 所在地圖 " + c.getPlayer().getMapId());
            FilePrinter.printError("PortalScriptManager.txt", "未處理的傳送腳本 " + portal.getScriptName() + " 所在地圖 " + c.getPlayer().getMapId());
        }
        clearScripts();
    }

    public final void clearScripts() {
        scripts.clear();
    }
}
