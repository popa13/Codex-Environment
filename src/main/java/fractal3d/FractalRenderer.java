package fractal3d;

import java.awt.image.BufferedImage;
import java.util.Set;

public final class FractalRenderer {
    public enum SetType { MANDELBROT, JULIA }

    private static final String[] BASIS_LABELS = {
            "1", "i1", "i2", "i1i2", "i3", "i1i3", "i2i3", "i1i2i3"
    };

    public static String[] basisLabels() {
        return BASIS_LABELS.clone();
    }

    public record Params(
            SetType setType,
            int power,
            int maxIterations,
            double bailout,
            double epsilon,
            int maxRaySteps,
            double scale,
            int unitX,
            int unitY,
            int unitZ,
            Tricomplex juliaConstant
    ) {}

    public BufferedImage render(int width, int height, Camera camera, Params params, Set<Integer> cancelToken) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Vec3 origin = camera.getPosition();
        Vec3 forward = camera.getForward();
        Vec3 right = camera.getRight();
        Vec3 up = camera.getUp();
        double fov = Math.toRadians(55);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!cancelToken.isEmpty()) {
                    return image;
                }
                double nx = (2.0 * (x + 0.5) / width - 1.0) * Math.tan(fov / 2.0) * width / height;
                double ny = (1.0 - 2.0 * (y + 0.5) / height) * Math.tan(fov / 2.0);
                Vec3 dir = forward.add(right.scale(nx)).add(up.scale(ny)).normalize();
                int rgb = traceRay(origin, dir, params);
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private int traceRay(Vec3 origin, Vec3 dir, Params p) {
        double t = 0.0;
        double hitT = -1;
        for (int s = 0; s < p.maxRaySteps; s++) {
            Vec3 sample = origin.add(dir.scale(t));
            if (isInside(sample, p)) {
                hitT = t;
                break;
            }
            t += p.epsilon;
            if (t > 10.0) {
                break;
            }
        }

        if (hitT < 0) {
            return 0xFF05070D;
        }

        Vec3 pos = origin.add(dir.scale(hitT));
        Vec3 n = estimateNormal(pos, p);
        Vec3 light = new Vec3(0.6, 0.7, 0.3).normalize();
        double diff = Math.max(0.1, n.dot(light));
        int shade = (int) Math.min(255, 40 + diff * 215);
        int r = Math.min(255, (int) (shade * 0.7));
        int g = Math.min(255, (int) (shade * 0.9));
        int b = shade;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private Vec3 estimateNormal(Vec3 p, Params params) {
        double e = params.epsilon * 1.5;
        double dx = density(p.add(new Vec3(e, 0, 0)), params) - density(p.add(new Vec3(-e, 0, 0)), params);
        double dy = density(p.add(new Vec3(0, e, 0)), params) - density(p.add(new Vec3(0, -e, 0)), params);
        double dz = density(p.add(new Vec3(0, 0, e)), params) - density(p.add(new Vec3(0, 0, -e)), params);
        return new Vec3(dx, dy, dz).normalize();
    }

    private double density(Vec3 pos, Params p) {
        return isInside(pos, p) ? 1.0 : 0.0;
    }

    private boolean isInside(Vec3 point, Params p) {
        Tricomplex position = toTricomplex(point, p);
        Tricomplex z = (p.setType == SetType.MANDELBROT) ? new Tricomplex() : position;
        Tricomplex c = (p.setType == SetType.MANDELBROT) ? position : p.juliaConstant;
        double bailoutSq = p.bailout * p.bailout;

        for (int i = 0; i < p.maxIterations; i++) {
            z = z.pow(p.power).add(c);
            if (z.normSquared() > bailoutSq) {
                return false;
            }
        }
        return true;
    }

    private Tricomplex toTricomplex(Vec3 point, Params p) {
        double[] coeffs = new double[8];
        coeffs[p.unitX] = point.x() / p.scale;
        coeffs[p.unitY] = point.y() / p.scale;
        coeffs[p.unitZ] = point.z() / p.scale;
        return new Tricomplex(coeffs);
    }
}
