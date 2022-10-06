function enter(pi) {
    var event = pi.getEvent("爬繩子");
    if (event !== null && event.isRunning() && event.isCharCorrect(pi.getPortal().getName(), pi.getMapId())) {
        pi.warp(pi.getMapId() == 109030003 ? 109050000 : (pi.getMapId() + 1), 0);
    } else {
        pi.warpS(pi.getMapId(), 0);
    }
}
