// Main routine for Daikon invariant detector
// For documentation, see file doc/daikon.html in the distribution.

package daikon;

import daikon.split.*;
import daikon.split.misc.*;
import daikon.inv.Invariant;
import daikon.inv.Invariant.OutputFormat;
import daikon.config.Configuration;

import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.lang.Thread;

import org.apache.oro.text.regex.*;
import org.apache.log4j.Category;
import gnu.getopt.*;
import utilMDE.*;

public final class Daikon {

  static {
    System.err.
      print("**************************************************\n" +
	    "*                     WARNING                    *\n" +
	    "**************************************************\n" +
	    "* You are using the REDESIGN version (V3) of the *\n" +
	    "* Daikon engine. Make sure this is what you want.*\n" +
	    "**************************************************\n");
    System.err.flush();
  }

  public final static String release_version = "2.3.10";
  public final static String release_date = "June 10, 2002";
  public final static String release_string
    = "Daikon version " + release_version
    + ", released " + release_date
    + "; http://pag.lcs.mit.edu/daikon.";

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  Controls whether conditional program points (see Daikon
   * manual) are displayed.
   **/
  public static boolean dkconfig_output_conditionals = true;

  // All these variables really need to be organized better.

  public final static String lineSep = Global.lineSep;

  public final static boolean disable_splitting = false;

  public static boolean disable_ternary_invariants = false;

  // Change this at your peril; high costs in time and space for "false",
  // because so many more invariants get instantiated.
  public final static boolean check_program_types = true;

  // Problem with setting this to true:
  //  get no invariants over any value that can ever be missing
  // Problem with setting this to false:
  //  due to differrent number of samples, IsEqualityComparison is
  //  non-transitive (that is specially handled in the code)
  public final static boolean invariants_check_canBeMissing = false;

  // Specialized version for array elements; only examined if
  // invariants_check_canBeMissing is false
  public final static boolean invariants_check_canBeMissing_arrayelt = true;

  public final static boolean disable_modbit_check_message = false;
  // Not a good idea to set this to true, as it is too easy to ignore the
  // warnings and the modbit problem can cause an error later.
  public final static boolean disable_modbit_check_error = false;

  // When true, don't print textual output.
  public static boolean no_text_output = false;

  // When true, show how much time each program point took.
  // Has no effect unless no_text_output is "false".
  public static boolean show_progress = false;

  // When true, don't print invariants when their controlling ppt
  // already has them.  For example, this is the case for invariants
  // in public methods which are already given as part of the object
  // invariant for that class.
  public static boolean suppress_implied_controlled_invariants = false;

  // When true, don't print EXIT invariants over strictly orig()
  // variables when the corresponding entry ppt already has the
  // invariant.
  public static boolean suppress_implied_postcondition_over_prestate_invariants = false;

  // When true, use the Simplify theorem prover (not part of Daikon)
  // to locate logically redundant invariants, and flag them as
  // redundant, so that they are removed from the printed output.
  public static boolean suppress_redundant_invariants_with_simplify = false;

  // Set what output style to use.  DAIKON is the default; ESC style
  // is based on JML; SIMPLIFY style uses first order logical
  // expressions with lots of parens
  public static OutputFormat output_style = OutputFormat.DAIKON;
  // public static OutputFormat output_style = OutputFormat.ESCJAVA;
  // public static OutputFormat output_style = OutputFormat.SIMPLIFY;

  // When true, output numbers of values and samples (also names of variables)
  public static boolean output_num_samples = false;

  public static boolean ignore_comparability = false;

  // Controls which program points/variables are used/ignored.
  public static Pattern ppt_regexp;
  public static Pattern ppt_omit_regexp;
  public static Pattern var_omit_regexp;

  // The invariants detected will be serialized and written to this
  // file
  public static File inv_file;

  // Whether we want the memory monitor activated
  private static boolean use_mem_monitor = false;

  // Whether Daikon should print its version number and date
  public static boolean noversion_output = false;

  // Public so other programs can reuse the same command-line options
  public static final String help_SWITCH = "help";
  public static final String ppt_regexp_SWITCH = "ppt";
  public static final String ppt_omit_regexp_SWITCH = "ppt_omit";
  public static final String list_type_SWITCH = "list_type";
  public static final String var_omit_regexp_SWITCH = "var_omit";
  public static final String no_text_output_SWITCH = "no_text_output";
  public static final String show_progress_SWITCH = "show_progress";
  public static final String suppress_cont_SWITCH = "suppress_cont";
  public static final String suppress_post_SWITCH = "suppress_post";
  public static final String suppress_redundant_SWITCH = "suppress_redundant";
  public static final String prob_limit_SWITCH = "prob_limit";
  public static final String esc_output_SWITCH = "esc_output";
  public static final String ioa_output_SWITCH = "ioa_output";
  public static final String java_output_SWITCH = "java_output";
  public static final String mem_stat_SWITCH = "mem_stat";
  public static final String simplify_output_SWITCH = "simplify_output";
  public static final String output_num_samples_SWITCH = "output_num_samples";
  public static final String noternary_SWITCH = "noternary";
  public static final String config_SWITCH = "config";
  public static final String config_option_SWITCH = "config_option";
  public static final String debugAll_SWITCH = "debug";
  public static final String debug_SWITCH = "dbg";
  public static final String files_from_SWITCH = "files_from";
  public static final String noversion_SWITCH = "noversion";


  // A pptMap which contains all the Program Points
  public static PptMap all_ppts;

  /** Debug tracer **/
  public static final Category debugTrace = Category.getInstance ("daikon.Daikon");

  static String usage =
    UtilMDE.join(new String[] {
      release_string,
      "Daikon invariant detector.",
      "Copyright 1998-2002",  // " by Michael Ernst <mernst@lcs.mit.edu>",
      "Usage:",
      "    java daikon.Daikon [flags...] files...",
      "  Each file is a declaration file or a data trace file; the file type",
      "  is determined by the file name (containing \".decls\" or \".dtrace\").",
      "  For a list of flags, see the Daikon manual, which appears in the ",
      "  Daikon distribution and also at http://pag.lcs.mit.edu/daikon/."},
                 lineSep);

  /**
   * The arguments to daikon.Daikon are file names; declaration file names end
   * in ".decls" and data trace file names end in ".dtrace".
   **/
  public static void main(String[] args)
  {
    // Read command line options
    Set[] files = read_options(args);
    Assert.assert(files.length == 4);
    Set decls_files = files[0];  // [File]
    Set dtrace_files = files[1]; // [File]
    Set spinfo_files = files[2]; // [File]
    Set map_files = files[3];    // [File]

    if (! noversion_output) {
      System.out.println(release_string);
    }

    // Load declarations and splitters
    PptMap all_ppts = load_decls_files(decls_files);
    load_spinfo_files(all_ppts, spinfo_files);
    load_map_files(all_ppts, map_files);
    // setup_splitters(all_ppts); // XXX splitters are not implemented yet

    // Infer invariants
    process_data(all_ppts, dtrace_files);

    // Display invariants
    if (output_num_samples) {
      System.out.println("The --output_num_samples debugging flag is on.");
      System.out.println("Some of the debugging output may only make sense to Daikon programmers.");
    }
    PrintInvariants.print_invariants(all_ppts);
    if (output_num_samples) {
      Global.output_statistics();
    }

    // Write serialized output
    if (inv_file != null) {
      try {
	FileIO.write_serialized_pptmap(all_ppts, inv_file);
      } catch (IOException e) {
	System.err.println("Error while writing '" + inv_file + "': " + e);
	System.exit(1);
      }
    }

    // Done
    System.out.println("Exiting");
  }


  ///////////////////////////////////////////////////////////////////////////
  // Read in the command line options
  // Return an array of {decls, dtrace, spinfo} filenames
  private static Set[] read_options(String args[])
  {
    if (args.length == 0) {
      System.out.println("Daikon error: no files supplied on command line.");
      System.out.println(usage);
      System.exit(1);
    }

    Set decl_files = new HashSet();
    Set dtrace_files = new HashSet();
    Set spinfo_files = new HashSet();
    Set map_files = new HashSet();

    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(help_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(ppt_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(ppt_omit_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(list_type_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(var_omit_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(no_text_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(show_progress_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(suppress_cont_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(suppress_post_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(suppress_redundant_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(prob_limit_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(esc_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(simplify_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(ioa_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(java_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(mem_stat_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(output_num_samples_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(noternary_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(config_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(config_option_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(debugAll_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(debug_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(files_from_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(noversion_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
    };
    Getopt g = new Getopt("daikon.Daikon", args, "ho:", longopts);
    int c;

    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 0:
	// got a long option
	String option_name = longopts[g.getLongind()].getName();
        if (help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          System.exit(1);
        } else if (ppt_regexp_SWITCH.equals(option_name)) {
          if (ppt_regexp != null)
            throw new Error("multiple --" + ppt_regexp_SWITCH
                            + " regular expressions supplied on command line");
          try {
            String regexp_string = g.getOptarg();
            // System.out.println("Regexp = " + regexp_string);
            ppt_regexp = Global.regexp_compiler.compile(regexp_string);
          } catch (Exception e) {
            throw new Error(e.toString());
          }
          break;
        } else if (ppt_omit_regexp_SWITCH.equals(option_name)) {
          if (ppt_omit_regexp != null)
            throw new Error("multiple --" + ppt_omit_regexp_SWITCH
                            + " regular expressions supplied on command line");
          try {
            String regexp_string = g.getOptarg();
            // System.out.println("Regexp = " + regexp_string);
            ppt_omit_regexp = Global.regexp_compiler.compile(regexp_string);
          } catch (Exception e) {
            throw new Error(e.toString());
          }
          break;
        } else if (list_type_SWITCH.equals(option_name)) {
          try {
            String list_type_string = g.getOptarg();
	    ProglangType.list_implementors.add(list_type_string);
	  } catch (Exception e) {
            throw new Error(e.toString());
          }
          break;
        } else if (var_omit_regexp_SWITCH.equals(option_name)) {
          if (var_omit_regexp != null)
            throw new Error("multiple --" + var_omit_regexp_SWITCH
                            + " regular expressions supplied on command line");
          try {
            String regexp_string = g.getOptarg();
            // System.out.println("Regexp = " + regexp_string);
            var_omit_regexp = Global.regexp_compiler.compile(regexp_string);
          } catch (Exception e) {
            throw new Error(e.toString());
          }
          break;
	} else if (debugAll_SWITCH.equals(option_name)) {
	  Global.debugAll = true;
	} else if (debug_SWITCH.equals(option_name)) {
	  Logger.setPriority(g.getOptarg(), Logger.DEBUG);
	} else if (no_text_output_SWITCH.equals(option_name)) {
	  no_text_output = true;
	} else if (show_progress_SWITCH.equals(option_name)) {
	  show_progress = true;
	} else if (suppress_cont_SWITCH.equals(option_name)) {
	  suppress_implied_controlled_invariants = true;
	} else if (suppress_post_SWITCH.equals(option_name)) {
	  suppress_implied_postcondition_over_prestate_invariants = true;
	} else if (suppress_redundant_SWITCH.equals(option_name)) {
	  suppress_redundant_invariants_with_simplify = true;
	} else if (prob_limit_SWITCH.equals(option_name)) {
	  double limit = Double.parseDouble(g.getOptarg());
	  if ((limit < 0.0) || (limit > 1.0)) {
	    throw new Error(prob_limit_SWITCH + " must be between [0..1]");
	  }
	  Configuration.getInstance().apply
	    ("daikon.inv.Invariant.probability_limit", String.valueOf(limit));
	} else if (esc_output_SWITCH.equals(option_name)) {
	  output_style = OutputFormat.ESCJAVA;
	} else if (simplify_output_SWITCH.equals(option_name)) {
	  output_style = OutputFormat.SIMPLIFY;
	} else if (ioa_output_SWITCH.equals(option_name)) {
	  output_style = OutputFormat.IOA;
	} else if (java_output_SWITCH.equals(option_name)) {
	  output_style = OutputFormat.JAVA;
	} else if (mem_stat_SWITCH.equals(option_name)) {
	  use_mem_monitor = true;
	} else if (output_num_samples_SWITCH.equals(option_name)) {
	  output_num_samples = true;
	} else if (noternary_SWITCH.equals(option_name)) {
	  disable_ternary_invariants = true;
        } else if (config_SWITCH.equals(option_name)) {
	  String config_file = g.getOptarg();
	  try {
	    InputStream stream = new FileInputStream(config_file);
	    Configuration.getInstance().apply(stream);
	  } catch (IOException e) {
	    throw new RuntimeException("Could not open config file " + config_file);
	  }
          break;
        } else if (config_option_SWITCH.equals(option_name)) {
	  String item = g.getOptarg();
	  Configuration.getInstance().apply(item);
          break;
        } else if (files_from_SWITCH.equals(option_name)) {
	  try {
	    BufferedReader files_from = UtilMDE.BufferedFileReader(g.getOptarg());
	    String filename;
	    while ((filename = files_from.readLine()) != null) {
	      // This code is duplicated below outside the options loop.
	      // These aren't "endsWith()" because there might be a suffix
	      // on the end (eg, a date).
	      File file = new File(filename);
	      if (! file.exists()) {
		throw new Error("File " + filename + " not found.");
	      }
	      if (filename.indexOf(".decls") != -1) {
		decl_files.add(file);
	      } else if (filename.indexOf(".dtrace") != -1) {
		dtrace_files.add(file);
	      } else if (filename.indexOf(".spinfo") != -1) {
		spinfo_files.add(file);
	      } else if (filename.indexOf(".map") != -1) {
		map_files.add(file);
	      } else {
		throw new Error("Unrecognized file extension: " + filename);
	      }
	    }
	  } catch (IOException e) {
	    throw new RuntimeException("Error reading --files_from file");
	  }
	  break;
	} else if (noversion_SWITCH.equals(option_name)) {
          noversion_output = true;
        } else {
	  throw new RuntimeException("Unknown long option received: " + option_name);
	}
	break;
      case 'h':
        System.out.println(usage);
        System.exit(1);
        break;
      case 'o':
	if (inv_file != null)
	  throw new Error("multiple serialization output files supplied on command line");

        String inv_filename = g.getOptarg();
        inv_file = new File(inv_filename);

        if (! UtilMDE.canCreateAndWrite(inv_file)) {
          throw new Error("Cannot write to file " + inv_file);
        }
        break;
        //
      case '?':
        break; // getopt() already printed an error
        //
      default:
        System.out.print("getopt() returned " + c + lineSep);
        break;
      }
    }

    // This code is duplicated above within the switch processing
    // First check that all the file names are OK, so we don't do lots of
    // processing only to bail out at the end.
    for (int i=g.getOptind(); i<args.length; i++) {
      File file = new File(args[i]);
      // These aren't "endsWith()" because there might be a suffix on the end
      // (eg, a date).
      if (! file.exists()) {
        throw new Error("File " + file + " not found.");
      }
      if (file.toString().indexOf(".decls") != -1) {
        decl_files.add(file);
      } else if (file.toString().indexOf(".dtrace") != -1) {
        dtrace_files.add(file);
      } else if (file.toString().indexOf(".spinfo") != -1) {
	spinfo_files.add(file);
      } else if (file.toString().indexOf(".map") != -1) {
	map_files.add(file);
      } else {
        throw new Error("Unrecognized argument: " + file);
      }
    }

    return new Set[] {
      decl_files,
      dtrace_files,
      spinfo_files,
      map_files,
    };
  }


  ///////////////////////////////////////////////////////////////////////////
  // Read decls, dtrace, etc. files

  private static PptMap load_decls_files(Set decl_files)
  {
    elapsedTime(); // reset timer
    try {
      System.out.print("Reading declaration files ");
      PptMap all_ppts = FileIO.read_declaration_files(decl_files);
      Dataflow.init_partial_order(all_ppts);
      all_ppts.trimToSize();
      System.out.print(" (read ");
      System.out.print(UtilMDE.nplural(decl_files.size(), "file"));
      System.out.println(")");
      return all_ppts;
    } catch (IOException e) {
      System.out.println();
      e.printStackTrace();
      throw new Error(e.toString());
    } finally {
      debugTrace.debug("Time spent on read_declaration_files: " + elapsedTime());
    }
  }

  private static void load_spinfo_files(PptMap all_ppts,
					Set spinfo_files // [File]
					)
  {
    elapsedTime(); // reset timer
    if (!disable_splitting && spinfo_files.size() > 0) {
      try {
	System.out.print("Reading splitter info files ");
	create_splitters(all_ppts, spinfo_files);
	System.out.print(" (read ");
	System.out.print(UtilMDE.nplural(spinfo_files.size(), "file"));
	System.out.println(")");
      } catch (IOException e) {
	System.out.println();
	e.printStackTrace();
	throw new Error(e.toString());
      } finally {
	debugTrace.debug("Time spent on load_spinfo_files: " + elapsedTime());
      }
    }
  }

  private static void load_map_files(PptMap all_ppts,
				     Set map_files // [File]
				     )
  {
    elapsedTime(); // reset timer
    if (!disable_splitting && map_files.size() > 0) {
      System.out.print("Reading map (context) files ");
      ContextSplitterFactory.load_mapfiles_into_splitterlist
	(map_files, ContextSplitterFactory.dkconfig_granularity);
      System.out.print(" (read ");
      System.out.print(UtilMDE.nplural(map_files.size(), "file"));
      System.out.println(")");
      debugTrace.debug("Time spent on load_map_files: " + elapsedTime());
    }
  }

  // XXX untested code
  private static void setup_splitters(PptMap all_ppts)
  {
    if (disable_splitting) {
      return;
    }

    for (Iterator itor = all_ppts.iterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();

      Splitter[] pconds = null;
      if (SplitterList.dkconfig_all_splitters) {
	pconds = SplitterList.get_all();
      } else {
	pconds = SplitterList.get(ppt.name);
      }
      if (pconds != null) {
	if (Global.debugSplit.isDebugEnabled()) {
	  Global.debugSplit.debug("Got "
				  + UtilMDE.nplural(pconds.length, "splitter")
				  + " for " + ppt.name);
	}
	ppt.add_splitters(pconds);
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Infer invariants over the trace data

  /** A way to output FileIO progress information easily */
  private final static Thread fileio_progress = new FileIOProgress();
  static class FileIOProgress extends Thread {
    public FileIOProgress() { setDaemon(true); }
    public void run() {
      DateFormat df = DateFormat.getTimeInstance(/*DateFormat.LONG*/);
      while (true) {
	System.out.print("\r[" + (df.format(new Date())) + "]: " + message());
	try {
	  sleep(5000);
	} catch (InterruptedException e) {
	  // hmm
	}
      }
    }
    private String message() {
      File file = FileIO.data_trace_filename;
      if (file == null) {
	return "[no status]";
      }
      LineNumberReader lnr = FileIO.data_trace_reader;
      String line = (lnr == null) ? "?" : String.valueOf(lnr.getLineNumber());
      return "Reading " + file.getName() + " (line " + line + ") ...";
    }
  }

  /**
   * The main data-processing routine of the daikon engine.  At this
   * point, the decls and spinfo files have been loaded, all of the
   * program points have been setup, and candidate invariants have
   * been instantiated.  This routine processes data to falsify the
   * candidate invariants.
   **/
  private static void process_data(PptMap all_ppts,
				   Set dtrace_files)
  {
    MemMonitor monitor = null;
    if (use_mem_monitor) {
      monitor = new MemMonitor("stat.out");
      new Thread((Runnable) monitor).start();
    }

    elapsedTime(); // reset timer
    try {
      System.out.println("Processing trace data; reading "
			 + UtilMDE.nplural(dtrace_files.size(), "file")
			 + ":");
      fileio_progress.start();
      FileIO.read_data_trace_files(dtrace_files, all_ppts);
      fileio_progress.stop();
      System.out.println();
      System.out.print("Creating implications "); // XXX untested code
      for (Iterator itor = all_ppts.iterator() ; itor.hasNext() ; ) {
	PptTopLevel ppt = (PptTopLevel) itor.next();
	System.out.print('.');
	// ppt.addImplications();
      }
      System.out.println();
    } catch (IOException e) {
      System.out.println();
      e.printStackTrace();
      throw new Error(e.toString());
    } finally {
      debugTrace.debug("Time spent on read_data_trace_files: " + elapsedTime());
    }

    if (monitor != null) {
      monitor.stop();
    }

    if (suppress_redundant_invariants_with_simplify) {
      System.out.print("Invoking Simplify to identify redundant invariants");
      System.out.flush();
      elapsedTime(); // reset timer
      for (Iterator itor = all_ppts.iterator() ; itor.hasNext() ; ) {
	PptTopLevel ppt = (PptTopLevel) itor.next();
	ppt.mark_implied_via_simplify(all_ppts);
        System.out.print(".");
        System.out.flush();
      }
      System.out.println(elapsedTime());
    }

  }


  ///////////////////////////////////////////////////////////////////////////
  //
  static public void create_splitters(PptMap all_ppts, Set spinfo_files)
    throws IOException
  {
    for (Iterator i = spinfo_files.iterator(); i.hasNext(); ) {
      File filename = (File) i.next();
      SplitterObject[][] splitterObjectArrays =
	SplitterFactory.read_spinfofile(filename, all_ppts);
      for (int j = 0; j < splitterObjectArrays.length; j++) {
	int numsplitters = splitterObjectArrays[j].length;
	String pptname = splitterObjectArrays[j][0].getPptName();
	Vector splitters = new Vector();
	for (int k = 0; k < numsplitters; k++) {
	  if (splitterObjectArrays[j][k].splitterExists()) {
	    splitters.addElement(splitterObjectArrays[j][k].getSplitter());
	  } else {
	    System.out.println(splitterObjectArrays[j][k].getError());
	  }
	}

	if (splitters.size() >= 1) {
	  // If the pptname is ALL, associate it with all program points.
	  if (pptname.equals("ALL")) {
	    SplitterList.put(".*", (Splitter[]) splitters.toArray(new Splitter[0]));
	  } else {
	    SplitterList.put( pptname, (Splitter[])splitters.toArray(new Splitter[0]));
	  }
	}
      }
    }
  }

  private static long elapsedTime_timer = System.currentTimeMillis();
  private static String elapsedTime() {
    long now = System.currentTimeMillis();
    double elapsed = (now - elapsedTime_timer) / 1000.0;
    String result = (new java.text.DecimalFormat("#.#")).format(elapsed) + "s";
    elapsedTime_timer = now;
    return result;
  }

}
