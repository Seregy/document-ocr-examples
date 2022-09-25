package com.seregy77.ocr.domain.document;

import java.math.BigDecimal;

public record BoundingBox(Position topLeft, Position topRight, Position bottomRight, Position bottomLeft) {
    public BigDecimal getWidth(BigDecimal documentWidth) {
        BigDecimal bottomWidth = bottomRight.x().subtract(bottomLeft.x()).multiply(documentWidth);
        BigDecimal topWidth = topRight.x().subtract(topLeft.x()).multiply(documentWidth);

        return bottomWidth.max(topWidth);
    }
}
