package ca.jvsh.photosharing;

import java.io.Serializable;

public class FileDataTransfer implements Serializable
{
	private static final long	serialVersionUID	= 1L;
	public boolean tcpOnly;
	public long fileSize;
	public String fileName;
	
	public FileDataTransfer (boolean tcpOnly, long fileSize, String fileName)
	{
		this.tcpOnly = tcpOnly;
		this.fileSize=fileSize;
		this.fileName=fileName;
	}
}
