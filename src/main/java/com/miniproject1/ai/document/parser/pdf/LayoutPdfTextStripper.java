package com.miniproject1.ai.document.parser.pdf;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.List;

/**
 * PDF 글자의 가로 위치를 이용해 표의 열 간격을 최대한 유지하는 추출기입니다.
 */
public class LayoutPdfTextStripper extends PDFTextStripper {

    private static final int COLUMN_GAP_THRESHOLD = 3;
    private static final int MAX_EXTRA_SPACES = 8;

    private float previousTextEndX = -1;

    public LayoutPdfTextStripper() {
        setSortByPosition(true);
    }

    /**
     * 이전 글자 묶음과 거리가 멀면 열 구분용 공백을 추가합니다.
     */
    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (!textPositions.isEmpty() && previousTextEndX >= 0) {
            TextPosition firstPosition = textPositions.get(0);
            float gap = firstPosition.getXDirAdj() - previousTextEndX;
            float spaceWidth = Math.max(firstPosition.getWidthOfSpace(), 1f);
            int extraSpaces = (int) (gap / spaceWidth);

            if (extraSpaces >= COLUMN_GAP_THRESHOLD) {
                getOutput().write(" ".repeat(Math.min(extraSpaces, MAX_EXTRA_SPACES)));
            }
        }

        super.writeString(text, textPositions);

        if (!textPositions.isEmpty()) {
            TextPosition lastPosition = textPositions.get(textPositions.size() - 1);
            previousTextEndX = lastPosition.getXDirAdj() + lastPosition.getWidthDirAdj();
        }
    }

    /**
     * 새로운 줄에서는 이전 줄의 가로 위치를 초기화합니다.
     */
    @Override
    protected void writeLineSeparator() throws IOException {
        previousTextEndX = -1;
        super.writeLineSeparator();
    }
}
