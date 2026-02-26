package fractal3d;

public final class Camera {
    private double yaw = 0.7;
    private double pitch = 0.4;
    private double distance = 4.0;
    private double panX = 0.0;
    private double panY = 0.0;

    public Vec3 getPosition() {
        double x = distance * Math.cos(pitch) * Math.sin(yaw);
        double y = distance * Math.sin(pitch);
        double z = distance * Math.cos(pitch) * Math.cos(yaw);
        return new Vec3(x + panX, y + panY, z);
    }

    public Vec3 getForward() {
        Vec3 target = new Vec3(panX, panY, 0.0);
        return target.subtract(getPosition()).normalize();
    }

    public Vec3 getRight() {
        Vec3 up = new Vec3(0, 1, 0);
        return getForward().cross(up).normalize();
    }

    public Vec3 getUp() {
        return getRight().cross(getForward()).normalize();
    }

    public void orbit(double deltaYaw, double deltaPitch) {
        yaw += deltaYaw;
        pitch = Math.max(-1.4, Math.min(1.4, pitch + deltaPitch));
    }

    public void zoom(double amount) {
        distance = Math.max(1.0, Math.min(20.0, distance * amount));
    }

    public void pan(double dx, double dy) {
        panX += dx;
        panY += dy;
    }
}
