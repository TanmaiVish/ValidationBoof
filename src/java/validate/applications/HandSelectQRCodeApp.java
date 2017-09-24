package validate.applications;

import boofcv.gui.BoofSwingUtil;
import georegression.struct.point.Point2D_F64;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static validate.misc.ParseHelper.skipComments;

public class HandSelectQRCodeApp extends HandSelectBase {
    public static final int TOTAL_QR_CORNERS = 13;

    public HandSelectQRCodeApp( File file ) {
        super(new SelectQrCodeCornerPanel(),file);

        JButton bShowHelp = new JButton("Show Help");
        bShowHelp.addActionListener(e-> showHelpDialog());
        BoofSwingUtil.invokeNowOrLater(()->{infoPanel.add(bShowHelp);infoPanel.validate();});
    }

    @Override
    public void process(File file, BufferedImage image) {
        SelectQrCodeCornerPanel gui = (SelectQrCodeCornerPanel)this.imagePanel;

        gui.reset();

        File f = selectOutputFile(file);

        if( f.exists() ) {
            System.out.println("   loading labeled "+f.getName());
            try {
                gui.markers.addAll(load(f));
            } catch( RuntimeException e ) {
                int dialogResult = JOptionPane.showConfirmDialog (gui, e.getMessage()+"\nDelete?","Delete Corrupted?",JOptionPane.YES_NO_OPTION);
                if(dialogResult == JOptionPane.YES_OPTION){
                    if( !f.delete() ) {
                        System.err.println("Failed to delete");
                    }
                }
            }
        }

        BoofSwingUtil.invokeNowOrLater(()->{
            infoPanel.setImageShape(image.getWidth(),image.getHeight());
            gui.setBufferedImage(image);
        });

    }

    @Override
    protected void handleFirstImage() {
        super.handleFirstImage();

        Preferences prefs = Preferences.userRoot().node(getClass().getName());
        boolean showHelp = prefs.getBoolean("Show_QR_Help",true);
        if( !showHelp )
            return;

        showHelpDialog();
    }

    private void showHelpDialog() {
        try {
            InputStream stream = getClass().getResourceAsStream("qrcode_click_order.png");
            if( stream == null )
                return;
            BufferedImage image = ImageIO.read(stream);

            ImageIcon icon = new ImageIcon(image);

            int dialogResult = JOptionPane.showConfirmDialog (gui, "Show this image again?","QR Help",
                    JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,icon);

            Preferences prefs = Preferences.userRoot().node(getClass().getName());
            if(dialogResult == JOptionPane.NO_OPTION){
                prefs.putBoolean("Show_QR_Help",false);
            } else {
                prefs.putBoolean("Show_QR_Help",true);
            }

        } catch (IOException ignore) {
        }
    }

    @Override
    public String getApplicationName() {
        return "Select QR Code Corners";
    }

    @Override
    public void setScale( double scale ) {
        SelectQrCodeCornerPanel gui = (SelectQrCodeCornerPanel)this.imagePanel;
        gui.setScale(scale);
    }

    @Override
    public void clearPoints() {
        SelectQrCodeCornerPanel gui = (SelectQrCodeCornerPanel)this.imagePanel;
        gui.clearPoints();
        gui.repaint();
    }

    @Override
    public void save() {
        SelectQrCodeCornerPanel gui = (SelectQrCodeCornerPanel)this.imagePanel;

        if( gui.markers.isEmpty() ) {
            System.out.println("Nothing to save");
            return;
        }

        File f = selectOutputFile(inputFile);
        System.out.println("** Saving to "+f.getPath());

        try {
            PrintStream out = new PrintStream(f);

            out.println("# Hand Labeled QR Code Finder Patterns");
            for (int i = 0; i < gui.markers.size(); i++) {
                QRCorners c = gui.markers.get(i);
                if( c.corners.size() != TOTAL_QR_CORNERS ) {
                    System.out.println("Skipping QR with not enough points");
                    continue;
                }

                out.println("message = "+c.message);

                for (int j = 0; j < c.corners.size(); j++) {
                    Point2D_F64 p = c.corners.get(j);
                    out.printf("%.20f %.20f",p.x,p.y);
                    if( j != c.corners.size()-1)
                        out.print(" ");
                }
                out.println();
            }
            out.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private List<QRCorners> load( File file ) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line = skipComments(reader);

            List<QRCorners> out = new ArrayList<>();
            while( line != null ) {
                QRCorners c = new QRCorners();

                String words[] = line.split(" ");
                if( words.length < 3 ) {
                    throw new RuntimeException("expected 3 or more words");
                }
                if( !words[0].equals("message") || !words[1].equals("=")) {
                    throw new RuntimeException("Expected 'message ='");
                }
                c.message = words[2];

                line = reader.readLine();
                if( line == null )
                    throw new RuntimeException("Premature end of file");
                words = line.split(" ");
                if( words.length != TOTAL_QR_CORNERS*2) {
                    throw new RuntimeException("Unexpected number of words for corners");
                }
                for (int i = 0; i < words.length; i += 2 ) {
                    Point2D_F64 p = new Point2D_F64();
                    p.x = Double.parseDouble(words[i]);
                    p.y = Double.parseDouble(words[i+1]);
                    c.corners.add(p);
                }

                line = reader.readLine();
                out.add(c);
            }

            reader.close();

            return out;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class QRCorners
    {
        public List<Point2D_F64> corners = new ArrayList<>();
        public String message = "UNKNOWN";
    }


    public static void main(String[] args) {
        new HandSelectQRCodeApp(null);
    }
}