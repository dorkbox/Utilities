package org.bouncycastle.math.ec;

public class ECAccessor {
    public static void setCoordSystem(ECCurve curve, int coordinateSystem) {
        curve.coord = coordinateSystem;

    }
}
