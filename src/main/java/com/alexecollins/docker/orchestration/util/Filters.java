package com.alexecollins.docker.orchestration.util;


import com.alexecollins.docker.orchestration.model.Conf;

import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

public final class Filters {
	private Filters() {
	}

	public static void filter(File file, FileFilter fileFilter, Properties properties) throws IOException {

		if (file == null) {
			throw new IllegalArgumentException("file is null");
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("file " + file + " does not exist");
		}
		if (fileFilter == null) {
			throw new IllegalArgumentException("fileFilter is null");
		}
		if (properties == null) {
			throw new IllegalArgumentException("properties is null");
		}

		if (file.isDirectory()) {
			//noinspection ConstantConditions
			for (File child : file.listFiles()) {
				filter(child, fileFilter, properties);
			}
		} else if (fileFilter.accept(file)) {
			final File outFile = new File(file + ".tmp");
			final BufferedReader in = new BufferedReader(new FileReader(file));
			try {
				final PrintWriter out = new PrintWriter(new FileWriter(outFile));
				try {
					String l;
					while ((l = in.readLine()) != null) {
						// ${...}
                        out.println(filter(l, properties));
					}
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}

			move(outFile, file);
		}
	}

    public static String filter(String l, Properties properties) {
        if (l.matches(".*\\$\\{.*\\}.*")) {
            for (Map.Entry<Object, Object> e : properties.entrySet()) {
                l = l.replace("${" + e.getKey() + "}", e.getValue().toString());
            }
        }
        return l;
    }

    public static Conf filter(Conf conf, Properties properties) {

        final Conf out = new Conf();

        out.getHealthChecks().getPings().addAll(conf.getHealthChecks().getPings());
        out.getLinks().addAll(conf.getLinks());
        for (String add : conf.getPackaging().getAdd()) {
            out.getPackaging().getAdd().add(Filters.filter(add, properties));
        }
        out.getPorts().addAll(conf.getPorts());
        if (conf.getTag() != null) {
            out.setTag(Filters.filter(conf.getTag(), properties));
        }
        out.getVolumesFrom().addAll(conf.getVolumesFrom());

        return out;
    }

    private static void move(File from, File to) throws IOException {
        //renaming over an existing file fails under Windows.
        to.delete();
		if (!from.renameTo(to)) {
			throw new IOException("failed to move " + from + " to " + to);
		}
	}

	static int maxKeyLength(Properties properties) {
		final TreeSet<Object> t = new TreeSet<Object>(new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
		t.addAll(properties.keySet());
		return t.last().toString().length();
	}
}
