package plugins.perrine.openimadisutilities;

import java.awt.GridLayout;


import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;

import icy.gui.dialog.MessageDialog;
import icy.plugin.abstract_.PluginActionable;
public class openimadis_login extends PluginActionable  {

	public void run() {

		try
		{

			//first sert up the gui (using javax swing)
			LoginDialog pd = new LoginDialog();
			int value = JOptionPane.showConfirmDialog(null, pd, "Connect to Server " , JOptionPane.PLAIN_MESSAGE);
			if(value != JOptionPane.OK_OPTION)
				return;
			// If ok then first we connect to the server using cid API
			ImageSpace ispace = ImageSpaceObject.getConnectionManager();
			//login using authcode (client token from webclient)
			boolean valid = ispace.login(pd.isSSL(), pd.getServerName(), pd.getPort(), pd.getAppId(), pd.getAuthCode());
			if (valid)
				MessageDialog.showDialog("You are now connected to server "+pd.getServerName()+". You can use download and upload records");
			else
				MessageDialog.showDialog("Invalid AuthToken");



		}
		catch(Exception e)
		{
			MessageDialog.showDialog(e.getMessage());
		}

	}


	private class LoginDialog extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2630793739219701234L; // this is for EClipse only and automartically generated
		private JTextField authCode;
		private JTextField myIP;

		public LoginDialog()
		{
			setLayout(new GridLayout(6, 2, 5, 5));

			setupUI();
		}
		private void setupUI()
		{

			//  This example is to be used with VM only
			JLabel authCodeLabel = new JLabel("Auth Code");
			authCode = new JTextField();
			
			JLabel IPLabel = new JLabel("Vm Server IP");
			myIP = new JTextField();
			myIP.setText("192.168.56.101");

			add(authCodeLabel);
			add(authCode);
			add(IPLabel);
			add(myIP);
			
			// IN ICY, clicking on OK on this UI will then, launch the Compute method
		}

		public String getServerName()
		{

			return this.myIP.getText();  
		}

		public String getAppId()
		{
			
			return "8W7Oyo6c4haAm2MAySnHH3oMlVCXxtNeCHKDezDq"; //client declared on my VM
		}

		public String getAuthCode()
		{
			return this.authCode.getText();
		}

		public int getPort()
		{
			return 8080; //this is dependnat of the use of Http or https (here http) 
		}

		public boolean isSSL()
		{
			return false; //because of the use of http (would be true for http).
		}


	}



}
