function enter(pi) {
    var returnMap = pi.getSavedLocation("PACHINKO");
    pi.clearSavedLocation("PACHINKO");
    if (returnMap < 0) {
        returnMap = 100000000;
    }
    var target = pi.getMap(returnMap);
    var portal;
    if (portal == null) {
        portal = target.getPortalByScriptName("Pachinko_port_in");
    }
    if (portal == null) {
        portal = target.getPortalByScriptName("pachinko00");
    }
    if (portal == null) {
        portal = target.getPortalByScriptName("pachinko01");
    }
    if (portal == null) {
        portal = target.getPortalByScriptName("pachinko02");
    }
    if (portal == null) {
        portal = target.getPortalByScriptName("pachinko03");
    }
    if (portal == null) {
        portal = target.getPortalByScriptName("pachinkoEnter");
    }
    if (pi.getMapId() != target.getId()) {
        pi.playPortalSE();
        pi.getPlayer().changeMap(target, portal);
    }
}
