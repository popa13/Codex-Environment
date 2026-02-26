package fractal3d;

import java.util.Arrays;

/**
 * Tricomplex number with basis [1, i1, i2, i1i2, i3, i1i3, i2i3, i1i2i3]
 * and i1^2 = i2^2 = i3^2 = -1, with commuting units.
 */
public final class Tricomplex {
    private static final int DIM = 8;
    private final double[] coeffs;

    public Tricomplex() {
        this.coeffs = new double[DIM];
    }

    public Tricomplex(double[] values) {
        if (values.length != DIM) {
            throw new IllegalArgumentException("Expected 8 coefficients.");
        }
        this.coeffs = Arrays.copyOf(values, DIM);
    }

    public static Tricomplex basis(int index, double value) {
        double[] c = new double[DIM];
        c[index] = value;
        return new Tricomplex(c);
    }

    public double get(int index) {
        return coeffs[index];
    }

    public Tricomplex add(Tricomplex other) {
        double[] out = new double[DIM];
        for (int i = 0; i < DIM; i++) {
            out[i] = this.coeffs[i] + other.coeffs[i];
        }
        return new Tricomplex(out);
    }

    public Tricomplex multiply(Tricomplex other) {
        double[] out = new double[DIM];
        for (int a = 0; a < DIM; a++) {
            if (coeffs[a] == 0.0) {
                continue;
            }
            for (int b = 0; b < DIM; b++) {
                if (other.coeffs[b] == 0.0) {
                    continue;
                }
                int index = a ^ b;
                int overlap = Integer.bitCount(a & b);
                double sign = (overlap % 2 == 0) ? 1.0 : -1.0;
                out[index] += sign * coeffs[a] * other.coeffs[b];
            }
        }
        return new Tricomplex(out);
    }

    public Tricomplex pow(int p) {
        if (p < 0) {
            throw new IllegalArgumentException("Exponent must be non-negative.");
        }
        Tricomplex result = basis(0, 1.0);
        Tricomplex base = this;
        int exp = p;
        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = result.multiply(base);
            }
            base = base.multiply(base);
            exp >>= 1;
        }
        return result;
    }

    public double normSquared() {
        double sum = 0.0;
        for (double c : coeffs) {
            sum += c * c;
        }
        return sum;
    }
}
