#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <signal.h>
#include <errno.h>
#include <thread>
extern "C" JNIEXPORT jintArray JNICALL Java_dev_nexterm_Pty_open(JNIEnv* e,jobject,jstring dir) {
 const char* path=e->GetStringUTFChars(dir,nullptr);
 int fd=posix_openpt(O_RDWR|O_NOCTTY|O_CLOEXEC);
 if(fd<0 || grantpt(fd)!=0 || unlockpt(fd)!=0) { if(fd>=0)close(fd); e->ReleaseStringUTFChars(dir,path); return nullptr; }
 char slave[256];
 if(ptsname_r(fd,slave,sizeof(slave))!=0){close(fd);e->ReleaseStringUTFChars(dir,path);return nullptr;}
 // Keep a slave descriptor open across fork. Otherwise the parent's first
 // nonblocking read can see EIO before the child gets scheduled to open it.
 int s=::open(slave,O_RDWR|O_NOCTTY|O_CLOEXEC);
 if(s<0){close(fd);e->ReleaseStringUTFChars(dir,path);return nullptr;}
 pid_t pid=fork();
 if(pid==0){
  if(setsid()<0 || ioctl(s,TIOCSCTTY,0)<0)_exit(126);
  fcntl(s,F_SETFD,0);
  if(dup2(s,0)<0 || dup2(s,1)<0 || dup2(s,2)<0)_exit(126);
  if(s>2)close(s);close(fd);
  if(chdir(path)!=0)_exit(126);
  setenv("HOME",path,1);setenv("TERM","xterm-256color",1);
  execl("/system/bin/sh","sh","-i",nullptr);_exit(127);
 }
 ::close(s);
 e->ReleaseStringUTFChars(dir,path);
 if(pid<0){close(fd);return nullptr;}
 fcntl(fd,F_SETFL,fcntl(fd,F_GETFL)|O_NONBLOCK);
 jint values[]={fd,pid};auto result=e->NewIntArray(2);e->SetIntArrayRegion(result,0,2,values);return result;
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_dev_nexterm_Pty_read(JNIEnv* e,jobject,jint fd){
 char b[8192];ssize_t n;do {n=::read(fd,b,sizeof(b));}while(n<0&&errno==EINTR);
 if(n<0&&(errno==EAGAIN||errno==EWOULDBLOCK))return e->NewByteArray(0);
 if(n<=0)return nullptr;auto a=e->NewByteArray(n);e->SetByteArrayRegion(a,0,n,reinterpret_cast<jbyte*>(b));return a;
}
extern "C" JNIEXPORT jint JNICALL Java_dev_nexterm_Pty_write(JNIEnv* e,jobject,jint fd,jbyteArray data){
 auto n=e->GetArrayLength(data);auto b=e->GetByteArrayElements(data,nullptr);int offset=0;
 while(offset<n){auto r=::write(fd,b+offset,n-offset);if(r<0&&errno==EINTR)continue;if(r<0&&(errno==EAGAIN||errno==EWOULDBLOCK))break;if(r<=0){offset=-1;break;}offset+=r;}
 e->ReleaseByteArrayElements(data,b,JNI_ABORT);return offset;
}
extern "C" JNIEXPORT void JNICALL Java_dev_nexterm_Pty_resize(JNIEnv*,jobject,jint fd,jint rows,jint cols){winsize w{};w.ws_row=rows;w.ws_col=cols;ioctl(fd,TIOCSWINSZ,&w);}
extern "C" JNIEXPORT jintArray JNICALL Java_dev_nexterm_Pty_size(JNIEnv* e,jobject,jint fd){winsize w{};if(ioctl(fd,TIOCGWINSZ,&w)!=0)return nullptr;jint values[]={w.ws_row,w.ws_col};auto result=e->NewIntArray(2);e->SetIntArrayRegion(result,0,2,values);return result;}
extern "C" JNIEXPORT void JNICALL Java_dev_nexterm_Pty_close(JNIEnv*,jobject,jint fd,jint pid){kill(-pid,SIGKILL);kill(pid,SIGKILL);::close(fd);std::thread([pid](){int status;while(waitpid(pid,&status,0)<0&&errno==EINTR){}}).detach();}
