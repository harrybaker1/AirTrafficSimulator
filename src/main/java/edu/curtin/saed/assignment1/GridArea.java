package edu.curtin.saed.assignment1;

import javafx.geometry.VPos;
import javafx.scene.canvas.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import java.util.*;

public class GridArea extends Pane
{
    private double gridWidth;
    private double gridHeight;
    private double gridSquareSize = 1.0; // Re-calculated
    private boolean gridLines = true;
    private Color captionColour = Color.WHITE;
    private List<GridAreaIcon> icons = new ArrayList<>();
    private Canvas canvas = null;

    public GridArea(double gridWidth, double gridHeight)
    {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    /**
     * Changes whether grid lines are visible.
     */
    public void setGridLines(boolean gridLines)
    {
        this.gridLines = gridLines;
    }

    /**
     * Retrieves a modifiable list of GridAreaIcons. Use this to _add_ new icon objects; e.g.:
     *
     * GridArea area = ...;
     * area.getIcons().add(new GridAreaIcon(...));
     * area.requestLayout();
     */
    public List<GridAreaIcon> getIcons()
    {
        return icons;
    }

    /**
     * Sets the colour used to display the caption text for each icon.
     */
    public void setCaptionColour(Color captionColour)
    {
        this.captionColour = captionColour;
    }

    /**
     * Redraws the grid area, either because the user is manipulating the window, OR because you've
     * called 'requestLayout()'.
     */
    @Override
    public void layoutChildren()
    {
        super.layoutChildren();
        if(canvas == null)
        {
            canvas = new Canvas();
            canvas.widthProperty().bind(widthProperty());
            canvas.heightProperty().bind(heightProperty());
            getChildren().add(canvas);
        }

        GraphicsContext gfx = canvas.getGraphicsContext2D();
        gfx.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());

        // First, calculate how big each grid cell should be, in pixels. (We do need to do this
        // every time we repaint the arena, because the size can change.)
        gridSquareSize = Math.min(getWidth() / gridWidth,
                                  getHeight() / gridHeight);

        if(gridLines)
        {
            // Draw the arena grid lines. This may help for debugging purposes, and just generally
            // to see what's going on.
            gfx.setStroke(Color.WHITESMOKE);

            for(double gridX = 0.0; gridX < gridWidth; gridX++) // Internal vertical grid lines
            {
                double x = (gridX + 0.5) * gridSquareSize;
                gfx.strokeLine(x, gridSquareSize / 2.0, x, (gridHeight - 0.5) * gridSquareSize);
            }

            for(double gridY = 0.0; gridY < gridHeight; gridY++) // Internal horizontal grid lines
            {
                double y = (gridY + 0.5) * gridSquareSize;
                gfx.strokeLine(gridSquareSize / 2.0, y, (gridWidth - 0.5) * gridSquareSize, y);
            }
        }

        // Draw all the images and their captions.
        for(var icon : icons)
        {
            if(icon.isShown())
            {
                drawIcon(gfx, icon);
            }
        }
    }

    /**
     * Draw a GridAreaIcon -- its image and caption -- at their proper location. Only
     * to be called from within layoutChildren().
     */
    private void drawIcon(GraphicsContext gfx, GridAreaIcon icon)
    {
        // Get the pixel coordinates representing the center of where the image is to be drawn.
        double x = (icon.getX() + 0.5) * gridSquareSize;
        double y = (icon.getY() + 0.5) * gridSquareSize;

        // We also need to know how "big" to make the image.
        var image = icon.getImage();
        double fullSizePixelWidth = image.getWidth();
        double fullSizePixelHeight = image.getHeight();

        // Apply scaling
        double scale = icon.getScale();

        double displayedPixelWidth, displayedPixelHeight;
        if (fullSizePixelWidth > fullSizePixelHeight)
        {
            // Image is wider than it is high
            displayedPixelWidth = gridSquareSize * scale;
            displayedPixelHeight = gridSquareSize * fullSizePixelHeight / fullSizePixelWidth * scale;
        }
        else
        {
            // Image is taller than it is wide
            displayedPixelHeight = gridSquareSize * scale;
            displayedPixelWidth = gridSquareSize * fullSizePixelWidth / fullSizePixelHeight * scale;
        }

        // Actually put the image on the screen.
        gfx.save();
        gfx.translate(x, y);
        gfx.rotate(icon.getRotation());
        gfx.drawImage(
            image,
            -displayedPixelWidth / 2.0,
            -displayedPixelHeight / 2.0,
            displayedPixelWidth,
            displayedPixelHeight);
        gfx.restore();

        // Draw the caption below the image.
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setTextBaseline(VPos.TOP);
        gfx.setStroke(captionColour);
        gfx.strokeText(icon.getCaption(), x, y + (gridSquareSize / 2.0) * scale);  // Scale caption as well
    }
}
