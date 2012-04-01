
#! /bin/sh
################################################################
#  Copyright (c) 2009 Quest Software Pty Ltd.
#  This source code is and remains the exclusive property of
#  Quest Software Pty Ltd.  All Rights Reserved
################################################################
{ 

	PS1=; 
	PS2=; 
	[ -z "$SUA_DEBUG" ]&&exec 2>/dev/null;
	[ -n "$SUA_DEBUG" ]&&exec 2>&1; 
	TERM=dumb; 
	PATH=/usr/bin:/bin stty -echo; 
	SUA_Trap() 
	{ echo "[I-0]:ERR:$1 signal received; quitting"; exit 1; };
	for Sig in HUP INT TERM ;
  	   do eval "SUA_Trap_$Sig() { SUA_Trap $Sig ; }; trap SUA_Trap_$Sig $Sig"; 
	   done; 
	       PS1=""; 
	       PS2=""; 
	       DoLog() { echo "[I-0]:LOG:$@"; }; 
	       SUA_Discovery_version="1.6.1"; 
	       DoLog "phase=discovery"; 
	       DoLog "SUA_Discovery_version=$SUA_Discovery_version"; 
	       SUA_Fail() { echo "[I-0]:ERR:$@; quitting"; exit 1; }; 
	       SUA_Tail1() { 
	                        { cat - ; echo "__EOD_SUA_Tail1__"; }|
					{ 
						SUA_PriorLine=""; 
						while read SUA_TailLine; 
						do [ "$SUA_TailLine" = "__EOD_SUA_Tail1__" ]&&echo "$SUA_PriorLine"; SUA_PriorLine="$SUA_TailLine"; done; 
				        }; 
			   }; 
	       CheckPerlBinVer() {
				      [ -z "$1" -o ! -f "$1" ]&&PerlVer=2&&return; [ ! -x "$1" ]&&PerlVer=8&&return; 
				      eval `$1 -e '$_ = $]; $SQ=sprintf("%c",39); s/\n//g; print "PerlRawVer=$SQ$_$SQ\n"; s/[^0-9]//g; s/^0*//; $_ = substr($_,0,4) + 0; print "PerlVer=$_\n";' 2>/dev/null`; 
				      if [ -z "$PerlVer" ]||[ "$PerlVer" -lt 5005 ] ; then 
				           PerlVer=9; 
					   elif [ -z "$SUA_PERL6_OK" -a "$PerlVer" -ge 6000 ] ; then 
					   PerlVer=9; 
				     fi; 
				}; 
		SUA_Which () { 
				 PathDirs=`IFS=: ; set -- $PATH; echo $*`; 
				 WhichFile=""; for File in $1 ; 
				 do for Dir in $PathDirs ; 
				     do [ -x $Dir/$File ]&&WhichFile=$Dir/$File&&break; done; 
				 done; 
				 echo "${WhichFile:-no $File in $PathDirs}";
				 [ -n "$WhichFile" ]; 
			 }; 
		PATH="/usr/bin:/bin:$PATH"; 
		SUA_os_cmds="uname hostname cat ls"; 
		for SUA_os_cmd in $SUA_os_cmds ;
			do SUA_Which $SUA_os_cmd>/dev/null 2>/dev/null||SUA_Fail "cannot find \"$SUA_os_cmd\" command in PATH $PATH"; done; 
			SUA_hostname=`hostname 2>/dev/null`; 
			SUA_uname_s=`uname -s 2>/dev/null`; 
			SUA_uname_r=`uname -r 2>/dev/null`; 
			SUA_uname_a=`uname -a 2>/dev/null`; 
			case $SUA_uname_s in 
			    Linux|HP-UX ) SUA_sh_type=sh;; 
			    * ) SUA_sh_type=ksh;; 
			esac; 
			export SUA_uname_s; SUA_ver="$SUA_uname_r"; 
			case $SUA_uname_s in 
			    AIX ) SUA_ver="`uname -v`.$SUA_uname_r";; 
			    OSF1 ) [ -x /usr/sbin/sizer ]&&set -- `/usr/sbin/sizer -v 2>/dev/null`&&SUA_ver="$4";; 
			esac; 
			DoLog "hostname=$SUA_hostname";
			DoLog "uname_a='$SUA_uname_a'"; 
			DoLog "OS=$SUA_uname_s"; 
			DoLog "OSversion=$SUA_ver"; 
			case "$SUA_uname_s" in 
			Linux ) 
			SUA_cpu_model=`while read ; do
set -- $REPLY
[ "$1 $2 $3" = "model name :" ]&&shift 3&&echo "$@"&&break
done< /proc/cpuinfo`; DoLog "cpu_model='$SUA_cpu_model'"; SUA_cpu_mhz=`while read ; do
set -- $REPLY
[ "$1 $2 $3" = "cpu MHz :" ]&&echo $4&&break
done< /proc/cpuinfo`; SUA_cpu_count=`while read ; do
set -- $REPLY
[ "$1" = "processor" ]&&eval "echo $(( $3 + 1 ))"
done< /proc/cpuinfo|SUA_Tail1`;
			DoLog "cpu_count=$SUA_cpu_count"; 
			SUA_etc_issue=`cat /etc/issue 2>/dev/null`; 
			SUA_etc_issue=`echo $SUA_etc_issue`; 
			DoLog "etc_issue='$SUA_etc_issue'"; 
			SUA_etc_release=`cat /etc/redhat-release /etc/S[uU]SE-release 2>/dev/null`; 
			SUA_etc_release=`echo $SUA_etc_release`; 
			DoLog "etc_release='$SUA_etc_release'";; 
			SunOS ) 
				SUA_release=`cat /etc/release 2>&1`; 
				SUA_release=`echo $SUA_release`; 
				DoLog "etc_release='$SUA_release'"; 
				SUA_isainfo=`SUA_Which isainfo 2>&1`; 
				[ -x "$SUA_isainfo" ]||SUA_isainfo=""; 
				    if [ -n "$SUA_isainfo" ] ; then 
				      DoLog "isainfo_k=`isainfo -k 2>/dev/null`"; 
				      DoLog "isainfo_n=`isainfo -n 2>/dev/null`"; 
				   else 
				      DoLog "isainfo_k=<not implemented>"; 
				      DoLog "isainfo_n=<not implemented>"; 
				    fi; 
				    SUA_isalist=`isalist 2>/dev/null`; 
				    DoLog "isalist='$SUA_isalist'"; 
				    DoLog "optisa=`optisa $SUA_isalist 2>/dev/null`";; 
			HP-UX ) 
			  DoLog "model=`model 2>/dev/null`";; 
			AIX ) 
			  DoLog "oslevel=`oslevel 2>/dev/null`"; 
			  SUA_bitmode=`getconf KERNEL_BITMODE 2>/dev/null`; 
			  [ "$SUA_bitmode" = 64 ]||SUA_bitmode=32; 
			      DoLog "kernel-bitmode=$SUA_bitmode"; 
			      SUA_bos_rte=`lslpp -cl bos.rte 2>/dev/null`; 
			      SUA_bos_rte=`echo $SUA_bos_rte`; 
			      DoLog "lslpp-bos-rte='$SUA_bos_rte'"; 
			      SUA_bos_64bit=`lslpp -cl bos.64bit 2>/dev/null`; 
			      SUA_bos_64bit=`echo $SUA_bos_64bit`; 
			      DoLog "lslpp-bos-64bit='$SUA_bos_64bit'";; 
			OSF1 ) 
			   DoLog "machine=`machine 2>/dev/null`"; 
			   DoLog "sizer-c='`/usr/sbin/sizer -c 2>/dev/null`'"; 
			   DoLog "sizer-p=`/usr/sbin/sizer -p 2>/dev/null`"; 
			   DoLog "sizer-v='`/usr/sbin/sizer -v 2>/dev/null`'"; 
			   DoLog "sizer-implver=`/usr/sbin/sizer -implver 2>/dev/null`";; 
		   esac; 
		DoLog "shell2try=$SUA_sh_type"; 
		SUA_sh="`SUA_Which $SUA_sh_type 2>/dev/null`"; 
		export SUA_sh; 
		DoLog "foundshell=$SUA_sh"; 
		DoLog "foundshell-details='`ls -Ll $SUA_sh 2>&1`'"; 
		SUA_foundshell_runs=0; 
		SUA_foundshell_works=1; 
		if SUA_random_nums=`$SUA_sh -c 'echo $RANDOM $RANDOM $RANDOM' 2>/dev/null` ; then 
		   SUA_foundshell_runs=1; 
		   if [ "$SUA_random_nums" = "" ] ; then 
		       SUA_foundshell_works=0; 
		   else 
		      DoLog "random_nums='$SUA_random_nums'"; 
		      set -- $SUA_random_nums; 
		      [ "$1" = "$2" -a "$2" = "$3" -a "$1" = "$3" ]&&SUA_foundshell_works=0; 
		   fi;
		fi; 
		DoLog "foundshell-runs=$SUA_foundshell_runs"; 
		DoLog "foundshell-works=$SUA_foundshell_works"; 
		case $SUA_uname_s in 
		    SunOS ) SUA_id_cmd=/usr/xpg4/bin/id;; 
		    * ) SUA_id_cmd=id;; 
	        esac; 
		SUA_id=`$SUA_id_cmd -u 2>/dev/null`||SUA_Fail "cannot find \"$SUA_id_cmd\" command in PATH $PATH"; 
		[ "$SUA_id" = "" ]&&SUA_Fail "\"$SUA_id_cmd\" not functioning properly"; 
		SUA_TMPDIR=${SUA_TMPDIR:-/tmp}; DoLog "tmp-dir=$SUA_TMPDIR"; 
		[ -d $SUA_TMPDIR ]||SUA_Fail "directory $SUA_TMPDIR does not exist"; 
		[ -w $SUA_TMPDIR ]||SUA_Fail "directory $SUA_TMPDIR is not writable"; 
		SUA_x="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; 
		SUA_x="$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x"; 
		SUA_x="$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x"; 
		SUA_x="$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x"; 
		SUA_x="$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x$SUA_x"; 
		SUA_DataFilePrefix="$SUA_TMPDIR/qsft-sua-spacetest-$$"; 
		SUA_DataFileTest=${SUA_DataFilePrefix}-*.tmp; 
		SUA_DataFile=""; 
		for Idx in 0 1 2 3 4 5 6 7 8 9 ; 
		    do SUA_DataFileTest=${SUA_DataFilePrefix}-${Idx}.tmp; 
		        [ -f $SUA_DataFileTest ]&&rm -f $SUA_DataFileTest 2>/dev/null; 
			[ -f $SUA_DataFileTest ]&&continue; 
			SUA_DataFile=$SUA_DataFileTest; break; 
		    done; 
		    [ -z "$SUA_DataFile" ]&&SUA_Fail "too many old test files in $SUA_TMPDIR"; 
		    echo "${SUA_x}OK"> $SUA_DataFile 2>/dev/null; 
		    [ -f $SUA_DataFile ]||SUA_Fail "cannot create file in $SUA_TMPDIR"; 
		    SUA_xTest=`cat $SUA_DataFile 2>/dev/null`; 
		    rm -f $SUA_DataFile 2>/dev/null; 
		    case "$SUA_xTest" in 
		    *OK ) 
		       DoLog "256kb-free-in-tmp-dir=1";; 
		       * )  SUA_Fail "insufficient free disk space in $SUA_TMPDIR";; 
		    esac; 
		    unset SUA_x SUA_xTest; 
		    SUA_use_perl=1;
		    PerlBinsToTry="/usr/bin/perl /opt/perl/bin/perl /usr/contrib/bin/perl /usr/local/bin/perl"; 
		    BestPerlBin=""; 
		    BestPerlVer=0; 
		    BestPerlRawVer=""; 
		    RejectedPerlBins=""; 
		    if [ "$SUA_PERL_BIN" = "/dev/null" ] ; then 
		         DoLog "SUA_PERL_BIN-setting=[value=$SUA_PERL_BIN use-sh=1]"; 
			 SUA_use_perl=0; 
		    elif [ -n "$SUA_PERL_BIN" ] ; then 
		         CheckPerlBinVer $SUA_PERL_BIN; 
			 if [ $PerlVer -ge 10 -a $PerlVer -gt $BestPerlVer ] ; then 
			    BestPerlBin=$SUA_PERL_BIN; 
			    BestPerlVer=$PerlVer; 
			    BestPerlRawVer="$PerlRawVer"; 
			    DoLog "SUA_PERL_BIN-setting=[value=$SUA_PERL_BIN valid=1 use-sh=0]"; 
			else 
			    RejectedPerlBins=$SUA_PERL_BIN; 
		        fi; 
		    fi; 
		    if [ $SUA_use_perl = 1 -a -z "$BestPerlBin" ] ; then 
		       SPBValue=$SUA_PERL_BIN; 
		       [ -z "$SUA_PERL_BIN" ]&&SPBValue="not-set"; 
		       DoLog "SUA_PERL_BIN-setting=[value=$SPBValue valid=0 use-sh=0]"; 
		    fi; 
		    if [ $SUA_use_perl = 1  -a  -z "$BestPerlBin" ] ; then 
		       for PerlBin in $PerlBinsToTry ; 
		           do [ ! -x "$PerlBin" -o "$PerlBin" = "$SUA_PERL_BIN" ]&&continue; 
			   CheckPerlBinVer $PerlBin; 
			   if [ $PerlVer -ge 10 -a $PerlVer -gt $BestPerlVer ] ; then 
			      BestPerlBin=$PerlBin; 
			      BestPerlVer=$PerlVer; 
			      BestPerlRawVer="$PerlRawVer"; 
			   else [ -n "$RejectedPerlBins" ]&&RejectedPerlBins="$RejectedPerlBins,"; 
			        RejectedPerlBins="$RejectedPerlBins$PerlBin"; 
			   fi; 
			   done; 
		    fi; 
		    SUA_perl=""; 
		    if [ $SUA_use_perl -gt 0 ] ; then 
		       if [ $BestPerlVer -gt 0 ] ; then 
		          SUA_perl=$BestPerlBin; 
			  DoLog "foundperl=[bin=$SUA_perl ver=$BestPerlVer raw-ver=$BestPerlRawVer]"; 
			  DoLog "foundperl-details='`ls -Ll $SUA_perl 2>/dev/null`'"; 
			  DoLog "rejected-perl-bins=$RejectedPerlBins"; 
			else 
			  SUA_use_perl=0; 
			fi; 
		    fi; 
		    DoLogDiscMethod() { echo "[M-1]:DSC:$@"; };
		    if [ $SUA_use_perl -gt 0 ] ; then 
		       DoLogDiscMethod "method=perl"; 
		       DoLogDiscMethod "bin-to-run=$SUA_perl"; 
		       DoLogDiscMethod "error-msg="; 
		    elif [ $SUA_foundshell_works -gt 0 ] ; then 
		       DoLogDiscMethod "method=shell"; 
		       DoLogDiscMethod "bin-to-run=$SUA_sh"; 
		       DoLogDiscMethod "error-msg="; 
		    else 
		       DoLogDiscMethod "method=invalid"; 
		       DoLogDiscMethod "bin-to-run="; 
		       DoLogDiscMethod "error-msg=no usable Perl or shell interpreter found"; 
		    fi; 
    }
