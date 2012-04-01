package com.siteview.agent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class AgentShell {
	private static Agent agent;

	public static void main(String[] arstring) {
		int argsCount = arstring.length;

		if (argsCount > 1) {
			print("����ѡ�����", true);
			return;
		}

		Properties props;
		try {
			props = ConfigLoader.Load(AgentShell.class,"/conf/agent.conf");
		} catch (FileNotFoundException e) {
			print("���������ļ�����,�ļ�������",true);
			return;
		} catch (IOException e) {
			print("��ȡ�����ļ�����",true);
			return;
		}
		
		agent = new Agent(props);
		if (argsCount < 1)
			doInteractiveMode();
		else
			doCommandMode(arstring[0]);
	}

	private static void doCommandMode(String cmd) {
		if (ShellValidator.Validate(cmd)) {
			boolean result = agent.Run(cmd);

			if (result)
				doInteractiveMode();
			else {
				return;
			}
		} else {
			print("����ģʽ������֤��ͨ��", true);
		}
	}

	private static void doInteractiveMode() {
		InputStreamReader inputstreamreader = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(inputstreamreader);

		print("agent>", false);
		String cmd = null;
		try {
			while ((cmd = reader.readLine()) != null) {
				if (cmd.equalsIgnoreCase("exit"))
					break;
				// ��֤������Ч��
				if (ShellValidator.Validate(cmd)) {
					agent.Run(cmd);
				} else {
					print("����ģʽ������֤��ͨ��", true);
				}
				print("agent>", false);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void print(String cmd, boolean isBreak) {
		if (isBreak)
			System.out.print(cmd + "\n");
		else
			System.out.print("\b\b\b\b\b\b" + cmd);
	}
}
