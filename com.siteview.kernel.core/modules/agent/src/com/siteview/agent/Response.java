package com.siteview.agent;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONValue;

import com.siteview.agent.info.Info;

/*
 * Response��ʽ���壺
 * ���϶�����Ϣ�������["ok",[{��1����Ϣ�������Լ���}],[��2����Ϣ��������Լ���],...]
 * �����Ϣ�������["ok",[{�������Լ���},{�������Լ���}]]
 * ������Ϣ�������["ok",[{�������Լ���}]]
 * ����������["error","������Ϣ"]
 */

public class Response {

	public static final Response Invalid = new Response() {
		public String toJson() {
			return "[\"error\",\"Invalid request\"]";
		}
		
		public String toString() {
			return toJson();
		}
	};
	private List<Info> infoList = new ArrayList<Info>();

	protected Response() {

	}

	public Response(Info info) {
		infoList.add(info);
	}

	public Response(Info[] infos) {
		for (Info info : infos)
			infoList.add(info);
	}

	public String toJson() {
		String result = "";		
		if (infoList.size() > 0) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0, count = infoList.size(); i < count; i++) {				
				sb.append("," + JSONValue.toJSONString(infoList.get(i).toMap()));
			}
			result = sb.toString().substring(1);
		}		
		return "[\"ok\",[" + result + "]]";
	}
	
	public String toString() {
		return toJson();
	}
}
