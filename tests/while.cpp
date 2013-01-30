#include <iostream>
using namespace std;

int IsOdd(int a) {
  return (a-((a/2)*2));
}

int main() {
  int a, b, c;

  a = 100;
  b = 0;
  c = 0;

  while( ((a>0) | (a==0)) ) {
    if ( (!(((a/2) * 2) - a)) ) // if a is even
      c=(c+1);
    else
      c=c;
    a = (a-1);
  }

  c = (c-2); // c = 49

  while(IsOdd(c)) {
    if ( (c==1) )
      break;
    else {
      b = (b+1);
      c = (c-2);
    }
  }
  cout<< b << endl;

}

