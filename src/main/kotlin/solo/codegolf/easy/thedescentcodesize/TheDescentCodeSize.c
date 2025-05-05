#include <stdio.h>

int main(){
    int i,m,h,p;
    while(1){
        m=p=0;
        for(i=0;i<8;i++){
            scanf("%d",&h);
            if(h>m){m=h;p=i;}
        }
        printf("%d\n",p);
    }
}