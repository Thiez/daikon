: # Use -*- Perl -*- without knowing its path
  eval 'exec perl -S -w -n $0 "$@"'
  if 0;
# merge-esc.pl -- Merge Daikon output into Java source code as ESC assnotations
# Michael Ernst <mernst@lcs.mit.edu>
# Time-stamp: <2001-07-18 18:22:20 mernst>

# The input is a Daikon output file.  Files from the current directory
# are rewritten into -escannotated versions (use the -r switch as the
# first thing on the command line to recursively search from the
# current directory).

use Carp;
use File::Find;


my $java_modifier_re = '\b(?:abstract|final|private|protected|public|static|strictfp|synchronized|transient)\b';
# my $java_modifers_re = '\s*(?:' . $java_modifier_re . '\s*)*';
# at least one modifier
my $java_modifers_plus_re = '\s*(?:' . $java_modifier_re . '\s*)+';
# matches a full line; groups = ($spaces, $mods, $body, $fieldname);

my $field_decl_re = '^(\s+)(' . $java_modifers_plus_re . ')([^=;]*\b(\w+)(?:\s*\[\])*\s*[;=].*)$';

my $warn_on_no_invariants;	# whether to warn if no invariants for a ppt.
my $merge_unexpressable;	# whether to merge unexpressible invariants;
				#   if false, they are simply discarded.
my $recursive;			# whether to look recursively for files.

BEGIN {
  $warn_on_no_invariants = 0;
  $merge_unexpressable = 1;
  $recursive = 0;

  if ($ARGV[0] eq '-r') {
    $recursive = 1;
    shift @ARGV;
  }
}

if ((/^Inv filename = /)
    || (/^Reading (declaration|data trace) files /)
    || (/^Read [0-9]+ declaration file/)
    || (/^    Samples breakdown: /)
    || (/^    Variables:/)
    || (/^      Unmodified variables: /)
    || (/^      Modified primitive variables: /)
    || (/^\[No views for /)
    || (/^esc_name =/)
    || (/^Variables not equal: /)
    || (/^Condition always satisfied: /)
    || (/^OneOf problem: /)
    || (/^Exiting$/)) {
  next;
}

if (/^===========================================================================$/) {
  undef($methodname);
  next;
}


if (/:::/) {
  if (defined($methodname)) {
    die "methodname is $methodname while reading header line:\n  $_";
  }
  $methodname = $_;
  chomp($methodname);
  $methodname =~ s/ *[0-9]+ samples?$//;
  next;
}

s/[ \t]+\([0-9]+ values?, [0-9]+ samples?\)$//;
# %raw contains the invariant text directly from the file.
# It is indexed by the complete program point name.
if (defined($methodname)) {
  $raw{$methodname} .= $_;
}


sub simplify_args( $ ) {
  my ($args) = @_;
  $args =~ s/^\s*\(\s*//;
  $args =~ s/\s*\)\s*$//;
  $args =~ s/\s+([\[\]])/$1/g;
  # remove "final" and such
  @args = split(/\s*,\s*/, $args);
  @newargs = ();
  for my $arg (@args) {
    # print "before: $arg\n";
    $arg =~ s/(^|\s)(\w+[\[\]]*)\s+\w+([\[\]]*)$/$1$2/;
    # print "after: $arg\n";
    push @newargs, $arg;
  }
  $newargs = "(" . join(", ", @newargs) . ")";
  return $newargs;
}

# Return true if the argumentes are the same modulo whitespace;
# also, names are permitted to match only up to a prefix.
sub approx_argsmatch($$) {
  my ($args1, $args2) = @_;
  # Remove parens
  $args1 =~ s/^\((.*)\)$/$1/;
  $args2 =~ s/^\((.*)\)$/$1/;
  # Split on commas
  @args1 = split(/\s*,\s*/, $args1);
  @args2 = split(/\s*,\s*/, $args2);
  # Compare
  if (scalar(@args1) != scalar(@args2)) {
    return 0;
  }
  for my $i (0..$#args1) {
    if (! approx_argmatch($args1[$i], $args2[$i])) {
      return 0;
    }
  }
  return 1;
}


# Return true if the arguments are the same or one is a prefix of the other.
sub approx_argmatch($$) {
  my ($x, $y) = @_;
  if ($x eq $y) {
    return 1;
  }
  if (($x eq "") || ($y eq "")) {
    return 0;
  }

  if (length($x) > length($y)) {
    ($x, $y) = ($y, $x);
  }
  if ($x eq substr($y, length($y)-length($x))) {
    return 1;
  }
  return 0;
}


sub is_non_supported_invariant( $ ) {
  my ($inv) = @_;
  return (($inv =~ /format_esc/)
	  || ($inv =~ /"null"/)
	  || ($inv =~ /\[\] ==/)
	  || ($inv =~ /~/)
	  || ($inv =~ /\bmax\(/)
	  || ($inv =~ /\bmin\(/)
	  || ($inv =~ /\bsum\(/)
	  || ($inv =~ /\\new\(/)
	  || ($inv =~ / has only one value/)
	  || ($inv =~ /\\old\([^\)]*\\old\(/)
	  || ($inv =~ /\\typeof\([^ ]*\.length/));
}


END {

  my %meth_ppt = ();
  for my $ppt (keys %raw) {
    my $methodname = $ppt;
    # Change "Foo.<init>" to "Foo.Foo".
    $methodname =~ s/^(\w+)\.<init>\($/$1.$1\(/;

    $methodname =~ s/\(([^\(\)]*)\).*$//;
    $newargs = simplify_args($1);
    $methodname .= $newargs;

    $meth_ppt{$methodname} = $ppt;

    # print "method: $methodname\n";
    # print "ppt: $ppt\n";
    # print $raw{$ppt};
  }

  my @javafiles = ();
  my $nodeep = !$recursive;
  find(sub {
    push @javafiles, $File::Find::name if (/\.java$/ && -f);
    $File::Find::prune = (-d && $nodeep && ($_ ne '.'));
  }, ".");

  for my $javafile (@javafiles) {
    @fields = ();		# only non-primitive fields
    @owned_fields = ();
    @final_fields = ();		# only non-primitive final fields
    open(GETFIELDS, "$javafile") or die "Cannot open $javafile: $!";
    while (defined($line = <GETFIELDS>)) {
      if ($line =~ /$field_decl_re/) {
	my $fieldname = $4;
	if (($line =~ /\[\s*\]/)
	    || ($line !~ /\b(boolean|byte|char|double|float|int|long|short)\b/)) {
	  # This is not a primitive field
	  push(@fields,$fieldname);
	  if ($line =~ /\[\s*\]/) {
	    push(@owned_fields, $fieldname);
	  }
	  if ($line =~ /\bfinal\b/) {
	    push(@final_fields, $fieldname);
	  }
	}
      }
    }
    close(GETFIELDS);

    open(IN, "$javafile") or die "Cannot open $javafile: $!";
    open(OUT, ">$javafile-escannotated") or die "Cannot open $javafile-escannotated: $!";

    # We assume that classes never have the same sans-package name
    my $classname = $javafile;
    $classname =~ s/\.java$//;
    $classname =~ s|^.+/([^/]+)$|$1|; # Strip directories

    while (defined($line = <IN>)) {
      if (($line !~ /;$/)
	  && ($line =~ /\b(?:public|private|protected)\b/)
	  && ($line =~ /\b(\w+)\s*(\([^\)]*\))/)) {
	# This looks like a declaration of method $methodname.
	# (Requires public or private or protected to avoid false alarms.)
	my $methodname = $1;
	my $args = $2;
	my $fullmethname = "$classname.$methodname";
	# print "Found $fullmethname in $line";
	my $simple_args = simplify_args($args);
	my $fullmeth = $fullmethname . $simple_args;
	my $prebrace;
	my $postbrace;
	# This is because "$)" in regexp screws up Emacs parser.
	my $eolre = "\\n?\$";
	my $need_newline = 1;
	if ($line =~ /^\s*\{.*$eolre/) {
	  $prebrace = "";
	  $postbrace = $line;
	  $need_newline = 0;
	} elsif ($line =~ /\babstract\b/i) {
	  $prebrace = "";
	  $postbrace = $line;
	  $need_newline = 0;
	} elsif ($line =~ /^(.*)(\{.*$eolre)/) {
	  $prebrace = $1;
	  $postbrace = $2;
	} elsif ($line !~ /\)/) {
	  die "Put all args on same line as declaration of $fullmeth";
	} else {
	  my $nextline = <IN>;
	  if ($nextline =~ /^\s*\{.*$eolre/) {
	    $prebrace = $line;
	    $postbrace = $nextline;
	    $need_newline = 0;
	  } elsif ($nextline =~ /^(.*)(\{.*$eolre)/) {
	    $prebrace = $line . $1;
	    $postbrace = $2;
	  } else {
	    die "Didn't find open curly brace in first two lines of method definition:\n  $line  $nextline";
	  }
	}
	print OUT $prebrace;
	my $found = "";
	for my $ppt (keys %raw) {
	  # print "Checking $fullmeth against $ppt\n";
	  my $ppt_fullmeth = $ppt;
	  $ppt_fullmeth =~ s/:::.*$//;
	  $ppt_methname = $ppt_fullmeth;
	  $ppt_methname =~ s/\(.*$//;
	  $ppt_args = $ppt_fullmeth;
	  $ppt_args =~ s/^.*\(/\(/;
	  if (($fullmeth eq $ppt_fullmeth)
	      || (($fullmethname eq $ppt_methname)
		  && approx_argsmatch($simple_args, $ppt_args))) {
	    # Skip @requires clauses for overridden methods which already
	    # have them; ESC doesn't allow them and they perhaps shouldn't hold.
	    my $no_requires = (($ppt =~ /\.equals\s*\(\s*java\.lang\.Object\b/)
			       || ($ppt =~ /\.toString\s*\(\s*\)/));
	    # add more tests here
	    my $overriding = ($no_requires || 0);
	    # print "overriding=$overriding for $ppt\n";
	    my $requires = ($overriding ? "also_requires" : "requires");
	    my $ensures = ($overriding ? "also_ensures" : "ensures");
	    my $modifies = ($overriding ? "also_modifies" : "modifies");
	    if ($need_newline) {
	      print OUT "\n";
	      if ($found) {
		# print OUT "/* Just found $ppt $ppt_fullmeth */\n";
		# print OUT "/* Already found$found */\n";
	      }
	    }
	    $found .= "  $ppt=$ppt_fullmeth";
	    if ($ppt =~ /:::ENTER/) {
	      if (! $no_requires) {
		for my $inv (split("\n", $raw{$ppt})) {
		  if (is_non_supported_invariant($inv)) {
		    if ($merge_unexpressable) {
		      print OUT "/*! $requires " . $inv . " */\n";
		    }
		  } else {
		    print OUT "/*@ $requires " . $inv . " */\n";
		  }
		}
	      }
	    } elsif ($ppt =~ /:::EXIT/) {
	      my $ppt_combined = $ppt;
	      $ppt_combined =~ s/(:::EXIT)[0-9]+$/$1/;
	      # If this is :::EXIT22 but :::EXIT exists, suppress this.
	      if (($ppt eq $ppt_combined)
		  || (! exists($raw{$ppt_combined}))) {
		for my $inv (split("\n", $raw{$ppt})) {
		  if ($inv =~ s/^      Modified variables: //) {
		    $inv =~ s/\[\]/[*]/g;
		    $inv =~ s/\[[^][]+\.\.[^][]+\]/[*]/g;
		    my @mods = split(/ /, $inv);
		    @mods = grep(!/\.class$/, @mods);
		    @mods = grep(!/\~/, @mods);

		    # was: @mods = grep(!/\[[^*]+\]/, @mods);
		    # better to a[n..] => a[*] than nothing at all
		    grep(s/\[(.*[^*a-zA-Z0-9._].*)\]/[*]/g, @mods);
		    grep(s/\[(.*\.\..*)\]/[*]/g, @mods);
		    # even better would be to collect the list of
		    # indicies which are modified, and create a
		    # \forall to specify that the rest aren't

		    # change ary[*].field to ary[*]
		    grep(s/\[\*\]\..*/[*]/g, @mods);

		    for my $field (@final_fields) {
		      @mods = grep(!/^this.$field$/, @mods);
		    }
		    if (scalar(@mods) > 0) {
		      print OUT "/*@ $modifies " . join(', ', @mods) . " */\n";
		    }
		  } elsif (is_non_supported_invariant($inv)) {
		    if ($merge_unexpressable) {
		      print OUT "/*! $ensures " . $inv . " */\n";
		    }
		  } else {
		    print OUT "/*@ $ensures " . $inv . " */\n";
		  }
		}
	      } else {
		# print OUT "Suppressing $ppt because of $ppt_combined\n";
	      }
	    } else {
	      die "What ppt? $ppt";
	    }
	  }
	}

	if ((! $found) & ($warn_on_no_invariants)) {
	  print "Warning:  no invariants for method $fullmeth on line $line";
	}

	# Take special action if this is a constructor.
	if (($methodname ne $classname) || (scalar(@owned_fields) == 0)) {
	  print OUT $postbrace;
	} elsif ($postbrace =~ /^(.*)(\}.*\n?)$/) {
	  print OUT "$1\n";
	  for my $field (@owned_fields) {
	    print OUT "/*@ set $field.owner = this */\n";
	  }
	  print OUT $2;
	} else {
	  print OUT $postbrace;
	  if (scalar(@owned_fields) > 0) {
	    # Skip over as many lines as possible, until I see a control
	    # structure or a block end or some such.  This attempts to set
	    # the owner annotation after as many lines as possible, and in
	    # particular to set it after the variable is set.
	    my $nextline;
	    while ((defined($nextline = <IN>))
		   && (! (
			  # entering a control structure; don't want to do
			  # assignments conditionally
			  ($nextline =~ /\b(if|while|for)\b|\}/)
			  # in a comment
			  || (($nextline =~ /\/\*(.*)/) && (! $1 =~ /\*\//))
			  # method call, but not an assignment
			  || (($nextline =~ /^[^=]*\(/)
			      && ($nextline !~ /\bthis\s*\(/))))) {
	      print OUT $nextline;
	    }
	    for my $field (@owned_fields) {
	      print OUT "/*@ set $field.owner = this */\n";
	    }
	    print OUT $nextline;
	  }
	}

	next;
      } elsif ($line =~ /(private|public|protected)/) {
	# print "No match on $line";
      }

      # This puts object invariants at the beginning.
      # Alternately, put them at the end.
      if ($line =~ /^[^\/]*\bclass\s+$classname\b/) {
	# Looks like the declaration of class $classname
	print OUT $line;
	if ($line !~ /\{/) {
	  my $nextline = <IN>;
	  if ($nextline !~ /\{/) {
	    die "Didn't find open curly brace in first two lines of class definition:\n  $line  $nextline";
	  }
	  print OUT $nextline;
	}
	for my $fullmeth ("$classname" . ":::OBJECT", "$classname" . ":::CLASS") {
	  if (defined($raw{$fullmeth})) {
	    for my $inv (split("\n", $raw{$fullmeth})) {
	      if (is_non_supported_invariant($inv)) {
	        if ($merge_unexpressable) {
		  print OUT "/*! invariant " . $inv . " */\n";
	        }
	      } else {
	        print OUT "/*@ invariant " . $inv . " */\n";
	      }
	    }
	  }
        }
	next;
      }

      if ($line =~ /$field_decl_re/) {
	my ($spaces, $mods, $body, $fieldname) = ($1, $2, $3, $4);
	$mods = "" unless defined($mods); # to prevent warnings
	my $is_owned = grep(/^$fieldname$/, @owned_fields);
	print OUT "$spaces/*@ spec_public */ $mods$body\n";
	if ($is_owned) {
	  print OUT "/*@ invariant $fieldname.owner == this */\n";
	} else {
	  # print "not owned: $fieldname @owned_fields\n";
	}
	next;
      }

      # default
      print OUT $line;
    }

    close IN;
    close OUT;
  }

  exit 0;			# success

}
