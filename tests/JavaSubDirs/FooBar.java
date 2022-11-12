package foobar;

import foobar.subdir1.Foo;
import foobar.subdir2.Bar;

public class FooBar
{
   public static void main(String[] args)
   {
      Foo foo = new Foo();
      Bar bar = new Bar();

      System.out.println(foo);
      System.out.println(bar);
   }
}
