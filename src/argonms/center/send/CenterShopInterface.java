/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.center.send;

import argonms.center.CenterServer;
import argonms.center.recv.ShopCenterPacketProcessor;
import argonms.center.recv.RemoteCenterPacketProcessor;
import argonms.center.CenterRemoteSession;

/**
 *
 * @author GoldenKevin
 */
public class CenterShopInterface extends CenterRemoteInterface {
	private String host;
	private int port;
	private ShopCenterPacketProcessor pp;

	public CenterShopInterface(CenterRemoteSession session) {
		super(session);
	}

	public void makePacketProcessor() {
		this.pp = new ShopCenterPacketProcessor(this);
	}

	public RemoteCenterPacketProcessor getPacketProcessor() {
		return pp;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setClientPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getClientPort() {
		return port;
	}

	public void disconnect() {
		CenterServer.getInstance().shopDisconnected();
	}
}
