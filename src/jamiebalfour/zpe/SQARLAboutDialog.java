package jamiebalfour.zpe;

import jamiebalfour.HelperFunctions;
import jamiebalfour.zpe.core.ZPE;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;


class SQARLAboutDialog extends JDialog {

    public SQARLAboutDialog() {

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setTitle("About SQARL Runtime");
        getContentPane().setLayout(new BorderLayout(0, 0));

        this.setMaximumSize(new Dimension(300, 300));

        JLabel lblNewLabel = new JLabel("About SQARL Runtime");
        getContentPane().add(lblNewLabel, BorderLayout.NORTH);
        lblNewLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 20));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(getContentPane().getBackground());
        scrollPane.setMaximumSize(new Dimension(300, 9999));

        JTextArea lblMainInformation = new JTextArea("");
        lblMainInformation.setAlignmentX(SwingConstants.CENTER);
        scrollPane.setViewportView(lblMainInformation);
        lblMainInformation.setEditable(false);
        lblMainInformation.setBackground(getContentPane().getBackground());


        String msg = "";

        msg += "SQARL Runtime powered by ZPE copyright Jamie Balfour 2020 - 2024\n\n";

        msg += "ZPE version " + ZPE.VERSION_NUMBER_MAJOR + "." + ZPE.VERSION_NUMBER_MINOR + " [" + ZPE.VERSION_NAME + "]\n";

        msg += "Copyright Jamie B Balfour 2011 - " + ZPE.YASS_VERSION_YEAR;

        msg += "\n\nFor more information visit\nhttps://www.jamiebalfour.scot/projects/zpe/";

        lblMainInformation.setText(msg);

        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.SOUTH);

        final JButton btnVisitWebsiteButton = new JButton("Visit website");
        btnVisitWebsiteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HelperFunctions.OpenWebsite("https://www.jamiebalfour.scot/projects/zpe/");
                close(btnVisitWebsiteButton);

            }
        });
        panel.add(btnVisitWebsiteButton);

        final JButton btnDoneButton = new JButton("Done");
        btnDoneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close(btnDoneButton);
            }
        });
        panel.add(btnDoneButton);
    }

    private static final long serialVersionUID = -5246323084901677773L;

    private void close(Component c) {

        Window w = SwingUtilities.getWindowAncestor(c);

        if (w != null) {
            w.setVisible(false);
        }
    }
}
