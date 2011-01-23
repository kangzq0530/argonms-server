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

package argonms.loading.reactor;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class ReactorStats {
	private int link;
	private Map<Integer, State> states;

	protected ReactorStats() {
		states = new HashMap<Integer, State>();
	}

	protected void setLink(int reactorid) {
		this.link = reactorid;
	}

	protected void addState(int stateid, State s) {
		states.put(Integer.valueOf(stateid), s);
	}

	protected int getLink() {
		return link;
	}

	public Map<Integer, State> getStates() {
		return states;
	}
}
