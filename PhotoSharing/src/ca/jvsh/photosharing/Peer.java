package ca.jvsh.photosharing;

import java.io.Serializable;
import java.net.InetAddress;

public class Peer implements Serializable
{

	private InetAddress	ip;
	private Integer		port;
	private boolean		checked;

	public Peer(InetAddress ip, Integer port)
	{
		this.ip = ip;
		this.port = port;
	}

	public InetAddress getIp()
	{
		return ip;
	}

	public void setIp(InetAddress ip)
	{
		this.ip = ip;
	}

	public boolean isChecked()
	{
		return checked;
	}

	public void setChecked(boolean checked)
	{
		this.checked = checked;
	}

	public Integer getPort()
	{
		return port;
	}

	public void setPort(Integer port)
	{
		this.port = port;
	}

}